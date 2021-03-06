package ca.mcgill.ecse211.game;

import ca.mcgill.ecse211.odometer.Odometer;
import ca.mcgill.ecse211.odometer.OdometerExceptions;
import ca.mcgill.ecse211.threads.SensorData;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * This class implements the navigation functionality of the robot. The travelTo() method 
 * moves the robot in Y direction first and then X direction to travel to the destination 
 * coordinate. It contains methods for for the robot to travel to a specific point on the map, 
 * go to the tunnel based on the parameters given, travel through the tunnel and find the ring set.
 * 
 * It also contains helper methods which facilitates the robot's movement.
 * 
 * @author Ajay Patel
 * @author Fandi Yi
 * @author Lucas Bellido
 * @author Tianzhu Fu
 * @author Nicolas Abdelnour
 * @author Wenzong Xia
 *
 */
public class Navigation {
	private static final int TUNNEL_SPEED = 250;
	private static final int FORWARD_SPEED = 120;
	private static final int ROTATE_SPEED = 80;
	private static final int ACCELERATION = 300;

	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	private Odometer odometer;
	private SensorData data;

	/**
	 * This navigation class constructor sets up our robot to begin navigating a
	 * particular map
	 * 
	 * @param leftMotor  The EV3LargeRegulatedMotor instance for our left motor
	 * @param rightMotor The EV3LargeRegulatedMotor instance for our right motor
	 */
	public Navigation(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor) throws OdometerExceptions {
		this.odometer = Odometer.getOdometer();
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.data = SensorData.getSensorData();
		for (EV3LargeRegulatedMotor motor : new EV3LargeRegulatedMotor[] { this.leftMotor, this.rightMotor }) {
			motor.stop();
			motor.setAcceleration(ACCELERATION);
		}
	}

	/**
	 * This method travel the robot to desired position by following the line
	 * (Always rotate 90 degree), along with a correction
	 * 
	 * When avoid=true, the nav thread will handle traveling. If you want to travel
	 * without avoidance, this is also possible. In this case, the method in the
	 * Navigation class is used.
	 * 
	 * @param x The x coordinate to travel to (in cm)
	 * @param y The y coordinate to travel to (in cm)
	 * @param   avoid: the robot will pay attention to the distance from ultrasonic
	 *          sensor to avoid abstacle when navigating
	 */
	public void travelTo(int x, int y) {
		double dX = x - odometer.getXYT()[0];
		double dY = y - odometer.getXYT()[1];

		double theta = odometer.getXYT()[2];

		if (dY > 0.1) {
			turnTo(0);
			theta = 0;
		} else if (dY < -0.1) {
			turnTo(180);
			theta = 180;
		}

		moveWithCorrection(dY, theta);
		odometer.setY(y);

		if (dX > 0.1) {
			turnTo(90);
			theta = 90;
		} else if (dX < -0.1) {
			turnTo(-90);
			theta = -90;
		}
		moveWithCorrection(dX, theta);
		odometer.setX(x);

	}

	/**
	 * Move a certain distance with correction (using coordinate system)
	 * 
	 * @param distance: distance to cover
	 * @param theta: theta to be corrected each time
	 */
	public synchronized void moveWithCorrection(double distance, double theta) {
		leftMotor.setSpeed(FORWARD_SPEED);
		rightMotor.setSpeed(FORWARD_SPEED);

		// correct error of the distance
		int tiles = Math.abs((int) Math.round(distance)) + 1;
		for (int i = 0; i < tiles; i++) {
			moveOneTileWithCorrection(theta);
		}
		leftMotor.rotate(-convertDistance(Game.WHEEL_RAD, Game.SEN_DIS), true);
		rightMotor.rotate(-convertDistance(Game.WHEEL_RAD, Game.SEN_DIS), false);
	}

	private void moveOneTileWithCorrection(double theta) {
		leftMotor.forward();
		rightMotor.forward();
		while (leftMotor.isMoving() || rightMotor.isMoving()) {
			double left = data.getL()[0];
			double right = data.getL()[1];
			if (left < -5) {
				leftMotor.stop(true);
			}
			if (right < -5) {
				rightMotor.stop(true);
			}
		}
		odometer.setTheta(theta);
	}
	
	
	public void moveOneTileWithCorrection() {
		leftMotor.forward();
		rightMotor.forward();
		while (leftMotor.isMoving() || rightMotor.isMoving()) {
			double left = data.getL()[0];
			double right = data.getL()[1];
			if (left < -5) {
				leftMotor.stop(true);
			}
			if (right < -5) {
				rightMotor.stop(true);
			}
		}
	}

	/**
	 * 	This method moves the robot forward inside the tunnel with correction
	 */
	public void moveWithCorrectionInTunnel() {
		moveOneTileWithCorrection();
		// increase the speed of the robot to prevent the ball bearing from getting stucked at the edge
		leftMotor.setSpeed(TUNNEL_SPEED);
		rightMotor.setSpeed(TUNNEL_SPEED);
		for (int i = 0; i < 2; i++) {
			moveOneTileWithCorrection();
		}
		leftMotor.setSpeed(FORWARD_SPEED);
		rightMotor.setSpeed(FORWARD_SPEED);
		// move to the grid line in front of the tunnel after it travels through it
		moveOneTileWithCorrection();
	}

	/**
	 * (*Improve* *Consider to discard*) This method is where the logic for the
	 * odometer will run. Use the methods provided from the OdometerData class to
	 * implement the odometer.
	 * 
	 * @param angle The angle we want our robot to turn to (in degrees)
	 * @param async whether return instantaneously
	 */
	public synchronized void turnTo(double angle) {
		double dTheta;

		dTheta = angle - odometer.getXYT()[2];
		if (dTheta < 0)
			dTheta += 360;

		// TURN RIGHT
		if (dTheta > 180) {
			leftMotor.setSpeed(ROTATE_SPEED);
			rightMotor.setSpeed(ROTATE_SPEED);
			leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 360 - dTheta), true);
			rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 360 - dTheta), false);
		}
		// TURN LEFT
		else {
			leftMotor.setSpeed(ROTATE_SPEED);
			rightMotor.setSpeed(ROTATE_SPEED);
			leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, dTheta), true);
			rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, dTheta), false);
		}
	}

	/**
	 * This method finds the tunnel based on the tunnel_ll and tunnel_ur coordinate,
	 * after the method, the robot will go the the entrance of the tunnel facing the
	 * tunnel
	 * 
	 * @param ll: lower left corner coordinate
	 * @param ur: upper right corner coordinate
	 */
	public void goToTunnel(int[] ll, int[] ur, int SC) {
		// find tunnel entrance
		if (GameParameter.GreenCorner == 0) {
			// case 1: starting corner at 0
			if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.NORTH) {
				// case 1.1: starting corner at 0 and tunnel is facing north
				travelTo(ll[0], ll[1] - 1);
				turnTo(90);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
				
			} else if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.EAST) {
				// case 1.2: starting corner at 0 and tunnel is facing east
				travelTo(ll[0] - 1, ll[1]);
				turnTo(90);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			}
			
		} else if (GameParameter.GreenCorner == 1) {
			// case 2: starting corner at 1
			if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.NORTH) {
				// case 2.1: starting corner at 1 and tunnel is facing north
				travelTo(ur[0], ll[1] - 1);
				turnTo(270);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			} else if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.WEST) {
				// case 2.2: starting corner at 1 and tunnel is facing west
				travelTo(ur[0] + 1, ur[1] - 1);
				turnTo(270);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			}
		} else if (GameParameter.GreenCorner == 2) {
			// case 3: starting corner at 2
			if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.SOUTH) {
				// case 3.1: starting corner at 2 and tunnel is facing south
				travelTo(ur[0], ur[1] + 1);
				turnTo(270);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			} else if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.WEST) {
				// case 3.2: starting corner at 2 and tunnel is facing west
				travelTo(ur[0] + 1, ur[1]);
				turnTo(270);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			}
		} else if (GameParameter.GreenCorner == 3) {
			// case 4: starting corner at 3
			if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.SOUTH) {
				// case 4.1: starting corner at 3 and tunnel is facing south
				travelTo(ur[0] - 1, ur[1] + 1);
				turnTo(90);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			} else if (GameParameter.determineTunnelHeading(ll, ur) == GameParameter.TunnelHeading.EAST) {
				// case 4.2: starting corner at 3 and tunnel is facing east
				travelTo(ll[0] - 1, ll[1] + 1);
				turnTo(90);
				leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
				moveOneTileWithCorrection();
				// move to the center of the tile
				leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), true);
				rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 5.5), false);
				leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
				rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
			}
		}
	}

	/**
	 * This method for go through the tunnel (call find tunnel before calling this
	 * method)
	 */
	public void goThroughTunnel(int[] ll, int[] ur) {

		moveWithCorrectionInTunnel();

		leftMotor.setSpeed(FORWARD_SPEED);
		leftMotor.setSpeed(FORWARD_SPEED);

		moveBackByOffset();
		// after passing through the tunnel, turn 90 degree to the right
		leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
		rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);

		selfLocalize();
	}

	/**
	 * This method performs a light localization on its current coordinate to correct its position
	 * it moves the robot forward until both light sensors at the back detects a line and move back by the offset
	 * distance to correct its position in one direction. It then turn to the left by 90 degree and moves forward
	 * to correct its heading in that direction. It then moves back by the offset distance and turn back to the 
	 * original heading
	 * 
	 */
	public void selfLocalize() {
		moveOneTileWithCorrection();
		moveBackByOffset();
		leftMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
		rightMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
		moveOneTileWithCorrection();
		moveBackByOffset();
		leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), true);
		rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, 90), false);
		moveOneTileWithCorrection();
		moveBackByOffset();
	}
	
	/**
	 * This method moves the robot forward after it navigates to the ring set (2 tiles away) to approach the ring set
	 * to perform the color detection
	 */
	public void approachRingSetForColorDetection() {
		moveOneTileWithCorrection();
		leftMotor.setSpeed(50);
		rightMotor.setSpeed(50);
		leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 19.05), true);
		rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 19.05), false);
	}

	/**
	 * This method moves the robot forward after it performs the color detection to retrieve the ring
	 */
	public void approachRingSetForRingRetrieval() {
		leftMotor.setSpeed(50);
		rightMotor.setSpeed(50);
		moveOneTileWithCorrection();
		leftMotor.rotate(convertDistance(Game.WHEEL_RAD, 1.5), true);
		rightMotor.rotate(convertDistance(Game.WHEEL_RAD, 1.5), false);
	}

	/**
	 * this method navigate the robot to the ring set, find the right position of
	 * the ring set
	 */
	public void goToRingSet(int[] TR) {
		double currentX = odometer.getXYT()[0];
		double currentY = odometer.getXYT()[1];
		
		if (Math.abs(currentX - TR[0]) < (0.2)) {
			if (currentY > TR[1]) {
				travelTo(TR[0], TR[1] + 2);
				turnTo(180);
				moveOneTileWithCorrection();

			} else if (currentY < TR[1]) {
				travelTo(TR[0], TR[1] - 2);
				turnTo(0);
				moveOneTileWithCorrection();

			}
		} else if (currentX < TR[0] && Math.abs(currentX - TR[0]) > 0.8) {
			travelTo(TR[0] - 2, TR[1]);
			turnTo(90);

			moveOneTileWithCorrection();

		} else if (currentX > TR[0] && Math.abs(currentX - TR[0]) > 0.8) {
			travelTo(TR[0] + 2, TR[1]);
			turnTo(270);
			moveOneTileWithCorrection();
		}
		moveBackByOffset();
		
		// perform a light localization before the ring set
		selfLocalize();

		Sound.beep();
		Sound.beep();
		Sound.beep();

	}
	
	/**
	 * This method moves the robot backward for one tile with correction
	 */
	public void backOffOneTileWithCorrection() {
		leftMotor.backward();
		rightMotor.backward();
		while (leftMotor.isMoving() || rightMotor.isMoving()) {
			double left = data.getL()[0];
			double right = data.getL()[1];
			if (left < -5) {
				leftMotor.stop(true);
			}
			if (right < -5) {
				rightMotor.stop(true);
			}
		}
	}
	
	
	/**
	 * This is a wrapper method whihc navigates the robot to the ring set and perform ring detection and retrieval on each side of the 
	 * ring set and the robot terminates at the initial position where it arrives at the ring set
	 */
	public void detectAndGrabRing() {
		
	}
	
	/**
	 * 	This method moves the robot backward by a distance of its sensor to the center of the wheel
	 */
	public void moveBackByOffset() {
		leftMotor.rotate(-convertDistance(Game.WHEEL_RAD, Game.SEN_DIS), true);
		rightMotor.rotate(-convertDistance(Game.WHEEL_RAD, Game.SEN_DIS), false);
	}

	/**
	 * Rotate the robot by certain angle
	 * 
	 * @param angle The angle to rotate our robot to
	 */
	public void turn(int angle) {
		leftMotor.rotate(convertAngle(Game.WHEEL_RAD, Game.TRACK, angle), true);
		rightMotor.rotate(-convertAngle(Game.WHEEL_RAD, Game.TRACK, angle), false);
	}

	/**
	 * This method allows the conversion of a distance to the total rotation of each
	 * wheel need to cover that distance.
	 * 
	 * @param radius   The radius of our wheels
	 * @param distance The distance traveled
	 * @return A converted distance
	 */
	public static int convertDistance(double radius, double distance) {
		return (int) ((180.0 * distance) / (Math.PI * radius));
	}

	/**
	 * This method allows the conversion of an angle value
	 * 
	 * @param radius   The radius of our wheels
	 * @param distance The distance traveled
	 * @param angle    The angle to convert
	 * @return A converted angle
	 */
	private static int convertAngle(double radius, double width, double angle) {
		return convertDistance(radius, Math.PI * width * angle / 360.0);
	}

}
