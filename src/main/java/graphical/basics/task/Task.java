package graphical.basics.task;

import graphical.basics.presentation.Presentation;

public interface Task {

    void setup();

    void step();

    boolean isDone();

    default Task andThen(Task t2) {
        if (this instanceof SequenceTask) {
            ((SequenceTask) this).addTask(t2);
            return this;
        } else {
            return new SequenceTask(this, t2);
        }
    }

    default Task parallel(Task t2) {
        if (this instanceof ParalelTask) {
            ((ParalelTask) this).addTask(t2);
            return this;
        } else {
            return new ParalelTask(this, t2);
        }
    }

    default Task repeat(int times) {
        return new RepeatTask(times, this);
    }

    default Task wait(int steps) {
        return this.andThen(new WaitTask(steps));
    }

    default Task step(Runnable runnable) {
        return this.andThen(new SingleStepTask(runnable));
    }

    static void consume(Task task) {
        task.setup();
        while (!task.isDone()) {
            task.step();
        }
    }

    default void execute() {
        Presentation.staticReference.execute(this);
    }

//
//    public static <T extends Task> Collector<T, ?, ParalelTask> toList() {
//        return new Collectors.CollectorImpl(ArrayList::new, List::add, (left, right) -> {
//            left.addAll(right);
//            return left;
//        }, CH_ID);
//    }

}
