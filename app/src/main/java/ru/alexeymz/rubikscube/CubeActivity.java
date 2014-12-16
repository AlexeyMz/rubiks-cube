package ru.alexeymz.rubikscube;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class CubeActivity extends Activity {

    private CubeSurfaceView glView;

    private MenuItem toggleUndoItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_cube);
        glView = new CubeSurfaceView(this);
        glView.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                String property = pce.getPropertyName();
                if (property.equals(CubeSurfaceView.IN_UNDO_MODE_PROPERTY)) {
                    toggleUndoItem.setChecked((Boolean)pce.getNewValue());
                }
            }
        });
        setContentView(glView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cube, menu);
        toggleUndoItem = menu.findItem(R.id.action_toggle_undo);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_randomize:
                glView.randomize(50);
                return true;
            case R.id.action_toggle_undo:
                glView.setInUndoMode(!glView.isInUndoMode());
                return true;
            case R.id.action_reset_view:
                glView.resetView();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
