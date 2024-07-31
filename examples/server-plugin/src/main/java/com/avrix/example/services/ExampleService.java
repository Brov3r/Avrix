package com.avrix.example.services;

/**
 * An example of implementing a service using an interface
 */
public class ExampleService implements Example {
    @Override
    public int getInt() {
        return 111;
    }

    @Override
    public void sayHello(String text) {
        System.out.println("text = " + text);
    }
}