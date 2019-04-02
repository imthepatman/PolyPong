package com.demgames.polypong;

import com.demgames.miscclasses.SendClasses.*;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassicGame extends ApplicationAdapter{
    private static final String TAG = "ClassicGame";
    //use of private etc is not consistently done
    private IGlobals globals;
    private String mode;
    private boolean gravityState,attractionState,agentmode;
    private int myPlayerNumber,numberOfPlayers,numberOfBalls,screenHeight, screenWidth,gameDialogState;

    private String[] playerNames;

    //setup global variables
    public ClassicGame(IGlobals globals_ , int myPlayerNumber_, int numberOfPlayers_, int numberOfBalls_, boolean gravityState_,
                       boolean attractionState_,String mode_, boolean agentmode_, int screenHeight_, int screenWidth_) {
        this.globals =globals_;
        this.myPlayerNumber = myPlayerNumber_;
        this.numberOfPlayers = numberOfPlayers_;
        this.numberOfBalls = numberOfBalls_;
        this.gravityState = gravityState_;
        this.attractionState = attractionState_;
        this.mode =mode_;
        this.agentmode =agentmode_;
        this.screenHeight = screenHeight_;
        this.screenWidth = screenWidth_;
    }

    //declare renderer and world related stuff
    private SpriteBatch spriteBatch;
    private PolygonSpriteBatch polygonSpriteBatch;
    private float width, height;
    private ShapeRenderer shapeRenderer;
    private Box2DDebugRenderer debugRenderer;
    private Matrix4 debugMatrix;
    private OrthographicCamera camera;

    private boolean allPlayersReady;

    List<String> notReadyPlayerList;

    private MiscObjects miscObjects;

    private ClassicGameObjects gameObjects;

    //stuff for potential use
    private long frameNumber=0;
    private int sendFrameSkip=1;
    private long currentMillis=System.currentTimeMillis();

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        //get width and height of displayGame
        this.width = 1f;
        this.height = 2f;

        //Gdx.app.debug("ClassicGame", " has focus " + globals.getSettingsVariables().gameHasFocus);

        this.allPlayersReady = false;
        this.notReadyPlayerList = new ArrayList<String>();
        this.playerNames = new String[this.numberOfPlayers];
        for(int i=0;i<this.numberOfPlayers;i++) {
            this.playerNames[i] = globals.getComm().playerMap.get(i).name;
        }

        this.miscObjects = new MiscObjects(globals,this.myPlayerNumber,this.width,this.height,this.screenHeight,this.screenWidth);

        this.gameObjects = new ClassicGameObjects(this.globals,this.myPlayerNumber,this.numberOfPlayers,
                this.playerNames, this.numberOfBalls, globals.getComm().balls,width,height, this.screenWidth, this.screenHeight, this.miscObjects,
                this.gravityState, this.attractionState,this.agentmode);

        this.miscObjects.setMaxZoom(this.gameObjects.gameField.gameFieldPolygon.getBoundingRectangle(),this.width);
        //setReceived fov of camera to displayGame
        this.camera = new OrthographicCamera(this.width, (float)this.screenHeight/ this.screenWidth*width);

        //setReceived position to middle of normal screen
        this.camera.zoom= miscObjects.maxZoomLevel;
        this.camera.position.set(0, -this.height/2+gameObjects.gameField.offset.y, 0);
        this.camera.update();

        //copy camera to debugmatrix for synchronous displaying of elements
        this.debugMatrix=new Matrix4(camera.combined);
        this.debugRenderer=new Box2DDebugRenderer();
        //shaperenderer for rendering shapes duuh
        this.shapeRenderer = new ShapeRenderer();
        //setReceived font and spriteBatch for drawing fonts and textures
        this.spriteBatch = new SpriteBatch();
        this.polygonSpriteBatch = new PolygonSpriteBatch();

        /*for(int i=0; i< globals.getSettingsVariables().numberOfPlayers;i++) {
            globals.getGameVariables().bats[i].batPosition = miscObjects.touches.touchPos[0];
        }*/

        globals.getComm().setGameState(0);
        this.gameDialogState = 0;

    }

    //executed when closed i think
    @Override
    public void dispose() {
        this.spriteBatch.dispose();
        this.polygonSpriteBatch.dispose();
        this.debugRenderer.dispose();
        this.gameObjects.dispose();
    }


    @Override
    public void render() {
        //Gdx.app.debug("ClassicGame", " has focus " + globals.getSettingsVariables().gameHasFocus);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {

            }
        });

        this.miscObjects.touches.checkTouches(gameObjects.gameField.offset,camera,gameObjects.fixedPoint);
        this.miscObjects.touches.checkZoomGesture();

        //Gdx.app.debug("ClassicGame", " touchPos "+miscObjects.touches.touchPos[0]);
        if(gameDialogState==0) {

            this.allPlayersReady = globals.getComm().checkAllClientConnectionStates(4);

            if(globals.getComm().gameState == 1) {

                //update from globals and update playerfields

                //dophysics
                gameObjects.updateAndSend(globals);

                if(!gameObjects.allBallsDestroyedState) {
                    gameObjects.doPhysics();

                }


            } else{
                this.notReadyPlayerList.clear();
                for(int i=0; i<this.numberOfPlayers;i++) {
                    if(globals.getComm().clientConnectionStatesMap.get(i)!=4) {
                        this.notReadyPlayerList.add(globals.getComm().playerMap.get(i).name);
                    }
                }

                Gdx.app.debug("ClassicGame", " not all players ready");
                for(int i=0; i<this.numberOfPlayers;i++){
                    Gdx.app.debug("ClassicGame", " state of player "+i + " : " + globals.getComm().clientConnectionStatesMap.get(i));
                }
            }
        }


        this.drawScreen();

        miscObjects.touches.updateLasts();
        frameNumber++;

        /** check gamedialogs **/
        if(globals.getComm().gameState!=1) {
            if(this.gameDialogState==0) {
                if(globals.getComm().gameState==0) {
                    this.gameDialogState =1;
                } else if(globals.getComm().gameState==3) {
                    this.gameDialogState =2;
                } else if(globals.getComm().gameState==4) {
                    this.gameDialogState =3;
                }

                if (this.gameDialogState == 1) {
                    globals.showStartDialog(new AlertDialogCallback() {

                        @Override
                        public void positiveButtonPressed() {
                            globals.getComm().sendObjectToAllClients(new SendConnectionState(myPlayerNumber,4), "tcp");
                            globals.getComm().clientConnectionStatesMap.put(myPlayerNumber,4);
                            gameDialogState = 0;
                            globals.getComm().setGameState(2);

                            Gdx.app.debug(TAG, "in gameState " + globals.getComm().gameState + " gameDialogState "+ gameDialogState);

                        }

                        @Override
                        public void negativeButtonPressed() {

                        }
                    });
                } else if (this.gameDialogState == 2) {
                    globals.getComm().clientConnectionStatesMap.put(myPlayerNumber, 5);
                    globals.getComm().sendObjectToAllClients(new SendConnectionState(myPlayerNumber, 5), "tcp");
                    globals.showPauseDialog(new AlertDialogCallback() {

                        @Override
                        public void positiveButtonPressed() {
                            globals.getComm().clientConnectionStatesMap.put(myPlayerNumber, 4);
                            globals.getComm().sendObjectToAllClients(new SendConnectionState(myPlayerNumber, 4), "tcp");
                            gameDialogState = 0;
                            globals.getComm().setGameState(2);

                            Gdx.app.debug(TAG, "in gameState " + globals.getComm().gameState + " gameDialogState "+ gameDialogState);
                        }

                        @Override
                        public void negativeButtonPressed() {

                        }
                    });

                } else if (this.gameDialogState == 3) {
                    globals.showEndDialog(new AlertDialogCallback() {

                        @Override
                        public void positiveButtonPressed() {
                            gameDialogState = 0;
                        }

                        @Override
                        public void negativeButtonPressed() {
                            globals.finishGDXGameLauncher();
                        }
                    });
                }
            }

        }

        if(!gameObjects.allBallsDestroyedState) {
            if(this.allPlayersReady) {
                globals.getComm().setGameState(1);
                Gdx.app.debug(TAG, "pre gameState " + globals.getComm().gameState + " gameDialogState "+ gameDialogState);

            } else {
                if(globals.getComm().gameState==1) {
                    globals.getComm().setGameState(3);
                }
            }
        } else {
            globals.getComm().setGameState(4);
        }

        Gdx.app.debug(TAG, "post gameState " + globals.getComm().gameState + " gameDialogState "+ gameDialogState);


    }

    void drawScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        //Gdx.gl.glDisable(GL20.GL_BLEND);
        this.camera.zoom= miscObjects.zoomLevel;
        //camera.position.setReceived(0,-height+height/2*miscObjects.zoomLevel+gameObjects.gameField.offset.y,0);
        this.camera.position.set(0,gameObjects.fixedPoint.y+miscObjects.zoomLevel*camera.viewportHeight/2,0);
        //Gdx.app.debug(TAG,Float.toString(gameObjects.fixedPoint.y));
        //Gdx.app.debug(TAG,Float.toString(camera.viewportHeight));
        //-height+height/2*miscObjects.zoomLevel
        this.camera.update();


        shapeRenderer.setProjectionMatrix(camera.combined);
        polygonSpriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.setProjectionMatrix(camera.combined);

        polygonSpriteBatch.begin();
        gameObjects.gameField.display(polygonSpriteBatch);
        polygonSpriteBatch.end();

        /*shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        miscObjects.touches.drawTouchPoints(shapeRenderer);
        shapeRenderer.end();*/

        spriteBatch.begin();
        gameObjects.displayGame(spriteBatch);
        gameObjects.displayUI(spriteBatch,this.notReadyPlayerList,this.allPlayersReady);
        //show fp
        //
        spriteBatch.end();

        //uncomment for box2d bodies to be shown
        //debugRenderer.render(gameObjects.world,camera.combined);
    }

}
