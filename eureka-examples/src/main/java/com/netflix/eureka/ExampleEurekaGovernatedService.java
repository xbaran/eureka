package com.netflix.eureka;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.guice.EurekaModule;
import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.netflix.governator.lifecycle.LifecycleManager;

/**
 * Sample Eureka service that registers with Eureka to receive and process requests, using EurekaModule.
 *
 * @author David Liu
 */
public class ExampleEurekaGovernatedService {

    static class ExampleServiceModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ExampleServiceBase.class).asEagerSingleton();
        }
    }

    private static ExampleServiceBase init() throws Exception {
        System.out.println("Creating injector for Example Service");

        // Simulated Child Injectors required for the optional @Injects in DiscoveryClient
        LifecycleInjectorBuilder builder = LifecycleInjector.builder().withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS);
        builder.withModules(
                Modules
                    .override(new EurekaModule())
                    .with(new AbstractModule() {
                      @Override
                      protected void configure() {
                          DynamicPropertyFactory configInstance = com.netflix.config.DynamicPropertyFactory.getInstance();
                          bind(DynamicPropertyFactory.class).toInstance(configInstance);
                          // the default impl of EurekaInstanceConfig is CloudInstanceConfig, which we only want in an AWS
                          // environment. Here we override that by binding MyDataCenterInstanceConfig to EurekaInstanceConfig.
                          bind(EurekaInstanceConfig.class).to(MyDataCenterInstanceConfig.class);

                          // (DiscoveryClient optional bindings) bind the optional event bus
                          // bind(EventBus.class).to(EventBusImpl.class).in(Scopes.SINGLETON);
                      }
                }),
                new ExampleServiceModule());

        Injector injector = builder.build().createInjector();
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManager.start();

        System.out.println("Done creating the injector");
        return injector.getInstance(ExampleServiceBase.class);
    }

    public static void main(String[] args) throws Exception {
        ExampleServiceBase exampleServiceBase = null;
        try {
            exampleServiceBase = init();
            exampleServiceBase.start();
        } catch (Exception e) {
            System.out.println("Error starting the sample service: " + e);
            e.printStackTrace();
        } finally {
            if (exampleServiceBase != null) {
                exampleServiceBase.stop();
            }
        }
    }

}
