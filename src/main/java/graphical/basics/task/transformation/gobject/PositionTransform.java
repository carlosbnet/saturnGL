package graphical.basics.task.transformation.gobject;

import graphical.basics.gobject.Gobject;
import graphical.basics.gobject.GroupGobject;
import graphical.basics.location.Location;
import graphical.basics.task.ParalelTask;
import graphical.basics.task.Task;
import graphical.basics.task.WaitTask;

import java.util.ArrayList;
import java.util.List;

public class PositionTransform implements Task {

    List<Location> locations;

    int steps;
    int stepCount;

    double aX;
    double aY;

    double deltaX;
    double deltaY;

    Gobject gobject;

    double amountX;
    double amountY;

    public PositionTransform(Gobject gobject, double amountX, double amountY, int steps) {
        this.gobject = gobject;
        this.steps = steps;
        this.amountX = amountX;
        this.amountY = amountY;
    }

    @Override
    public void setup() {
        stepCount = 0;
        locations = gobject.getRefereceLocations();
        aX = (4 * amountX) / (2 * steps + (steps * steps));
        aY = (4 * amountY) / (2 * steps + (steps * steps));

        deltaX = 0;
        deltaY = 0;
    }

    @Override
    public void step() {

        if (stepCount < (steps / 2)) {
            deltaX += aX;
            deltaY += aY;
            for (int i = 0; i < locations.size(); i++) {
                locations.get(i).incrementX(deltaX);
                locations.get(i).incrementY(deltaY);
            }

        } else {

            for (int i = 0; i < locations.size(); i++) {
                locations.get(i).incrementX(deltaX);
                locations.get(i).incrementY(deltaY);
            }
            deltaX -= aX;
            deltaY -= aY;
        }

        stepCount++;
    }

    public static Task delayed(Gobject gobject, double amountX, double amountY, int steps, int delay) {
        List<Task> tasks = new ArrayList<>();
        if (gobject instanceof GroupGobject) {
            int i=1;
            for (Gobject go : ((GroupGobject) gobject).getGobjects()) {
                tasks.add(new WaitTask(i*delay).andThen(new PositionTransform(go, amountX, amountY, steps)));
                i++;
            }
            return new ParalelTask(tasks);
        }
        return null;
    }

    @Override
    public boolean isDone() {
        return stepCount == steps;
    }
}
