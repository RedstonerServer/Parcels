package io.dico.dicore.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.NoSuchElementException;

public abstract class BaseTask<T> {
    private boolean running = false;
    private Integer taskId = null;
    private long workTime = 5L;
    private int workCount;

    public void start(Plugin plugin, int delay, int period, long workTime) {
        doStartChecks();
        this.workTime = workTime;
        workCount = 0;
        running = true;
        
        if (delay == -1) {
            run();
            if (!running) {
                return;
            }
            delay = period;
        }
        
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::run, delay, period);
    }
    
    public void startImmediately(Plugin plugin, int period, long workTime) {
        start(plugin, -1, period, workTime);
    }
    
    protected void doStartChecks() {
        if (isRunning()) {
            throw new IllegalStateException("Can't start when already running");
        }
    }

    public void start(Plugin plugin) {
        start(plugin, -1, 20, 5L);
    }

    protected void onFinish(boolean early) {
    }

    protected long getWorkTime() {
        return workTime;
    }

    protected abstract boolean process(T object);

    private void run() {
        workCount++;
        final long stop = System.currentTimeMillis() + getWorkTime();
        do {
            if (!processNext()) {
                return;
            }
        } while (System.currentTimeMillis() < stop);
    }

    public int getTaskId() {
        return running ? taskId : -1;
    }
    
    public int getWorkCount() {
        return workCount;
    }

    public boolean isRunning() {
        return running;
    }

    protected abstract T supply() throws NoSuchElementException;

    private void cancelTask(boolean early) {
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        running = false;
        taskId = null;
        onFinish(early);
    }

    private boolean processNext() {
        T object;
        try {
            object = supply();
        } catch (NoSuchElementException e) {
            cancelTask(false);
            return false;
        }

        try {
            if (process(object)) {
                return true;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        cancelTask(true);
        return false;
    }

}
