package com.ultreon.mods.lib.client.devicetest;

public final class DesktopApplication extends Application {
    private DesktopWindow desktop;
    private TaskbarWindow taskbar;

    public DesktopApplication() {
        super(id());
    }

    public static ApplicationId id() {
        return new ApplicationId("com.ultreon.apps:desktop");
    }

    @Override
    public boolean isOpenOnlyOne() {
        return true;
    }

    @Override
    public void create() {
        super.create();

        this.desktop = this.createDesktop();
        this.desktop.create();
        this.desktop.setBottomMost(true);

        this.taskbar = this.createTaskbar();
        this.taskbar.create();
        this.taskbar.setTopMost(true);
    }

    @Override
    public void update() {
        if (!this.taskbar.isValid()) {
            this.taskbar = this.createTaskbar();
            this.taskbar.create();
            this.taskbar.setTopMost(true);
        }
        if (!this.desktop.isValid()) {
            this.desktop = this.createDesktop();
            this.desktop.create();
            this.desktop.setBottomMost(true);
        }
    }

    private DesktopWindow createDesktop() {
        return new DesktopWindow(this);
    }

    private TaskbarWindow createTaskbar() {
        return new TaskbarWindow(this, 20);
    }
    
    public DesktopWindow getDesktop() {
        return desktop;
    }

    public TaskbarWindow getTaskbar() {
        return taskbar;
    }
}
