package com.example.jmeter.plugin.utils;

public class customCounter {
    private long counter;
    private final long start;
    private final Long end;
    private final int increment;

    public customCounter(Long start, Long end, int increment) {
        this.start = start;
        this.end = end;
        this.increment = increment;
        this.counter = start;
    }

    public long get() {
        return this.counter;
    }

//    public synchronized long setAndGet(Long newStart, Long newEnd) {
//        if (newStart != null && !newStart.equals(this.start))
//            counter = newStart;
//        if (newEnd != null && !newEnd.equals(this.end))
//            this.end = newEnd;
//        return this.counter;
//    }

    public long getAndAdd() {
        long oldValue = this.counter;
        this.counter += this.increment;
        if (this.end != null && this.counter > this.end)
            this.counter = this.start;
        return oldValue;
    }

    public long addAndGet() {
        this.counter += this.increment;
        if (this.end != null && this.counter > this.end)
            this.counter = this.start;
        return this.counter;
    }

    public long resetAndGet() {
        this.counter = this.start;
        return this.counter;
    }
}
