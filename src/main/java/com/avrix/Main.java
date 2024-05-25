package com.avrix;

import zombie.network.GameServer;

public class Main {
    public static void main(String[] args) {
        switch (args[0]) {
            case "-launch" -> GameServer.main(args);
            case "-install" -> System.out.println("Install");
            case "-uninstall" -> System.out.println("Uninstall");
        }
    }
}