package com.avrix;

import com.avrix.bootstrap.Bootstrap;
import com.avrix.bootstrap.DefaultBootstrap;
import com.avrix.provider.ZomboidGameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) {
        Bootstrap bootstrap = new DefaultBootstrap();

        try {
            bootstrap.initialize(new ZomboidGameProvider());
            bootstrap.launch(args);
        } catch (Exception e) {
            log.error("The application could not be launched!", e);
        }
    }
}