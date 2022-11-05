package org.tensorflow.demo.env;

import android.os.SystemClock;

public class SplitTimer {

  private long lastWallTime;
  private long lastCpuTime;

  public SplitTimer(final String name) {
    newSplit();
  }

  public void newSplit() {
    lastWallTime = SystemClock.uptimeMillis();
    lastCpuTime = SystemClock.currentThreadTimeMillis();
  }

  public void endSplit(final String splitName) {
    final long currWallTime = SystemClock.uptimeMillis();
    final long currCpuTime = SystemClock.currentThreadTimeMillis();

    lastWallTime = currWallTime;
    lastCpuTime = currCpuTime;
  }
}
