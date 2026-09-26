package com.anlyflad.core.raster;

public interface ParallelRunner {
    void run(int taskCount, Task task);

    interface Task {
        void run(int index);
    }

    ParallelRunner SEQUENTIAL=new ParallelRunner() {
        public void run(int taskCount, Task task) {
            for (int index=0;index<taskCount;index++) {
                task.run(index);
            }
        }
    };
}
