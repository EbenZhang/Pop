package com.ezhang.pop.core;

public interface ICallable<I, O> {
    public O Call(I input);
}