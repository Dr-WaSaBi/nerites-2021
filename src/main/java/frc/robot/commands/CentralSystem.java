/*----------------------------------------------------------------------------*/
/* Copyright (c) 2019 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot.commands;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj2.command.CommandBase;

import frc.robot.Constants;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Pulley;
import frc.robot.subsystems.Shooter;
import frc.robot.util.ballSystem.Ball;
import frc.robot.util.ballSystem.BallHandler;

// TODO: Fix this

/**
 * Adapted from https://github.com/frc1711/Nerites/blob/master/src/main/java/frc/robot/commands/CentralSystem.java.
 * @author Lou DeZeeuw (original)
 * @author Gabriel Seaver (adaptation)
 */
public class CentralSystem extends CommandBase {
    
    private Pulley pulley;
    private Shooter shooter;
    private BallHandler ballHandler;
    private Intake intake;
    private Joystick stick;
    
    private boolean pastMiddle;
    private boolean shootMode;
    private boolean hold;
    private boolean created;
    private boolean reverse;
    private boolean destroyed;
    private boolean manual;
    
    private int timeout;
    private int timeSinceStart;
    
    public CentralSystem (Pulley pulley, Shooter shooter, Intake intake, Joystick stick) {
        addRequirements(pulley, shooter, intake);
        
        this.pulley = pulley;
        this.intake = intake;
        this.shooter = shooter;
        this.stick = stick;
        
        timeout = 100;
        
        defaultButtons();
        manual = false;
        
        ballHandler = new BallHandler();
    }
    
    @Override
    public void initialize () {
        timeSinceStart = 0; 
        pulley.stop(); 
        shooter.stopShooter(); 
        shooter.stopFlyWheel(); 
    }
    
    private void flipButtons () {
        if (stick.getRawButtonReleased(4))
            reverse = !reverse;
        
        if (stick.getRawButtonReleased(8)) {
            manual = !manual;
            defaultButtons();
        }
    }
    
    private void automatedPulley () {
        if (pulley.getBottomSensor() && !created) {
            ballHandler.addBall(new Ball());
            created = true;
            timeSinceStart = 0;
        }
        
        Ball lastBall = ballHandler.getLastBallHandled();
        
        if (pulley.getMiddleSensor()) {
            if(!pastMiddle && ballHandler.numBallsInRobot() > 0) {
                lastBall.setPastSensor(true);
                pastMiddle = true;
            }
        } else {
            pastMiddle = false;
        }
        
        if(ballHandler.numBallsInRobot() == 1) {
            if(!lastBall.getPastSensor()) {
                pulley.run(.25);
            } else {
                pulley.stop();
                created = false;
            }
        } else if (ballHandler.numBallsInRobot() > 1 &&
                ballHandler.getSecondToLastBallHandled().getPastSensor() &&
                !lastBall.getPastSensor()) {
            pulley.run(Constants.pulleySpeed);
        } else {
            pulley.stop();
            created = false;
        }
        
        if (timeSinceStart > timeout)
            pulley.stop();
    }
    
    private void automatedShooter () {
        if (stick.getRawButtonReleased(1)) {
            shootMode = !shootMode;
            hold = !hold;
        }
        
        if (hold) {
            shooter.shoot(Constants.shooterSpeed);
            shootMode = true;
        } else {
            shooter.stopShooter();
        }
        
        if (!pulley.getTopSensor()) {
            destroyed = false;
            if (shootMode) {
                pulley.run(Constants.pulleySpeed + .1);
            }
        }
        
        if (pulley.getTopSensor()) {
            pulley.stop();
            if (!destroyed) {
                ballHandler.removeHighestBall();
                destroyed = true;
            }
        }
        
        if (ballHandler.numBallsInRobot() == 0)
            shootMode = false;
    }
    
    private void flyWheel () {
        if (!manual && stick.getRawButton(2)) {
            shooter.runFlyWheel();
        } else {
            shooter.stopFlyWheel();
        }
    }
    
    private void removeAllBalls (Boolean bool) {
        if (bool) ballHandler.removeHighestBall();
    }
    
    private void intake () {
        if (!pulley.getBottomSensor() || manual) {
            if (stick.getRawButton(5))
                intake.run(Constants.intakeSpeed);
            else if (stick.getRawButton(6))
                intake.run(-.75 * Constants.intakeSpeed);
            else
                intake.stop();
        } else if (stick.getRawButton(6))
            intake.run(-.75 * Constants.intakeSpeed);
        else
            intake.stop();
    }
    
    private void reverse () {
        pulley.run(-1 * Constants.pulleySpeed); 
        
        if (pulley.getBottomSensor() && !destroyed) {
            ballHandler.removeHighestBall();
            destroyed = true;
        }
        
        if(!pulley.getBottomSensor())
            destroyed = false;
    }
    
    private void manualPulley () {
        if (stick.getRawButton(4))
            pulley.run(-1 * Constants.pulleySpeed);
        else if (stick.getRawButton(3))
            pulley.run(Constants.pulleySpeed);
        else
            pulley.run(0);
    }
    
    private void manualShooter () {
        if (stick.getRawButton(1))
            shooter.shoot(Constants.shooterSpeed);
        else
            shooter.shoot(0);
    }
    
    private void defaultButtons () {
        shootMode = false;
        destroyed = false;
        created = false;
        pastMiddle = false;
        reverse = false;
        hold = false;
    }
    
    @Override
    public void execute () {
        timeSinceStart ++;
        flipButtons();
        removeAllBalls(stick.getRawButton(3));
        intake();
        flyWheel();
        
        if (!manual) {
            if (!reverse) {
                automatedPulley();
                automatedShooter();
            } else
                reverse();
        } else {
            manualPulley();
            manualShooter();
            removeAllBalls(true);
        }
    }
    
    @Override
    public void end (boolean interrupted) {
        pulley.stop();
    }
    
    @Override
    public boolean isFinished() {
        return false;
    }
    
}