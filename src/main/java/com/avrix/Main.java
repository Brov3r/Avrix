package com.avrix;

import zombie.network.GameServer;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world! Args: " + Arrays.toString(args));
        GameServer.main(args);
    }
}