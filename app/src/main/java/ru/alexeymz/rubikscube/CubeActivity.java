package ru.alexeymz.rubikscube;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CubeActivity extends Activity {

    private GLSurfaceView glView;

    private class CubeSurfaceView extends GLSurfaceView {
        private CubeSurfaceView(Context context) {
            super(context);
            setEGLContextClientVersion(2);
            setRenderer(new GLSurfaceView.Renderer() {
                @Override
                public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                    // Set the background frame color
                    GLES20.glClearColor(100 / 255f, 149 / 255f, 237 / 255f, 1.0f);
                }

                @Override
                public void onSurfaceChanged(GL10 unused, int width, int height) {
                    GLES20.glViewport(0, 0, width, height);
                }

                @Override
                public void onDrawFrame(GL10 unused) {
                    // Redraw background color
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                }
            });
            // Render the view only when there is a change in the drawing data
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_cube);
        glView = new CubeSurfaceView(this);
        setContentView(glView);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cube, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
