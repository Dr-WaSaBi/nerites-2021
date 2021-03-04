// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.auton;

import edu.wpi.first.wpilibj2.command.CommandBase;
import swerve.drive.FESwerveDrive;

public class AutonDriveCommand extends CommandBase {
    
    private static enum Phases {
        TURNING,
        DRIVING,
        ENDING
    }
    private static final double DIRECTION_MARGIN_OF_ERROR = 5;
    
    private final FESwerveDrive swerveDrive;
    private Phases phase;
    private final double
            direction,
            distance,
            speed;
    
    /**
     * Strafes the swerve drive in a certain direction, at a certain speed, over a certain distance.
     * @param _swerveDrive   The {@link FESwerveDrive} drive train
     * @param _direction     The direction, in degrees, to travel in. Zero degrees corresponds with
     * directly forward, and an increase in {@code direction} corresponds with a direction further
     * clockwise from a top-down view. This value must be on the interval [0, 360).
     * @param _distance      The distance to travel in the specified direction, in inches. This value
     * must be on the interval (0, infinity).
     * @param _speed         The speed to travel at. This value must be on the interval (0, 1].
     */
    public AutonDriveCommand (FESwerveDrive _swerveDrive, double _direction, double _distance, double _speed) {
        swerveDrive = _swerveDrive;
        phase = Phases.DRIVING;
        direction = _direction;
        distance = _distance;
        speed = _speed;
    }
    
    @Override
    public void initialize () {
        swerveDrive.stopMotor();
        swerveDrive.setDistanceReference(); // TODO: Remove this
    }
    
    @Override
    public void execute () {
        if (phase == Phases.TURNING) executeTurning();
        else if (phase == Phases.DRIVING) executeDriving();
    }
    
    private void executeTurning () {
        if (swerveDrive.steerAllWithinRange(direction, DIRECTION_MARGIN_OF_ERROR)) {
            phase = Phases.DRIVING;
            swerveDrive.setDistanceReference();
        }
    }
    
    private void executeDriving () {
        if (swerveDrive.getDistanceTraveled() < distance) {
            swerveDrive.steerAndDriveAll(direction, speed);
        } else {
            phase = Phases.ENDING;
        }
    }
    
    @Override
    public void end (boolean interrupted) {
        swerveDrive.stopMotor();
    }
    
    @Override
    public boolean isFinished () {
        return phase == Phases.ENDING;
    }
    
}