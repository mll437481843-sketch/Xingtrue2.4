package com.bilisave.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bilisave.app.data.Models;
import com.bilisave.app.data.Prefs;
import com.bilisave.app.data.Task;

import java.util.ArrayList;
import java.util.List;

public final class AppState {
    public static final AppState I = new AppState();

    public final ArrayList<Task> tasks = new ArrayList<>();
    public Models.Video video;
    public int partIndex = 0;
    public int qn = 0;
    public List<Models.Format> formats = new ArrayList<>();
    public boolean audioOnly = false;
    public String userName = "";
    public String userFace = "";
    public boolean loggingIn = false;

    public interface Listener {
        void onTasks();

        void onVideo();

        void onMsg(String msg);

        void onUser();
    }

    private final List<Listener> listeners = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    private AppState() {
    }

    public void add(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void remove(Listener l) {
        listeners.remove(l);
    }

    public void load(Context c) {
        tasks.clear();
        tasks.addAll(Task.listFromJson(Prefs.tasksJson(c)));
        for (Task t : tasks) if (Task.RUNNING.equals(t.state) || Task.MERGING.equals(t.state) || Task.PENDING.equals(t.state))
            t.state = Task.INTERRUPTED;
    }

    public void save(Context c) {
        Prefs.setTasksJson(c, Task.listToJson(tasks));
    }

    public void tasksChanged() {
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) l.onTasks();
        });
    }

    public void videoChanged() {
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) l.onVideo();
        });
    }

    public void userChanged() {
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) l.onUser();
        });
    }

    public void msg(String s) {
        main.post(() -> {
            for (Listener l : new ArrayList<>(listeners)) l.onMsg(s);
        });
    }
}

