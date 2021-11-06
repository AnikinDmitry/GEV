package org.cytoscape.app.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;

import java.util.Properties;

public class CyActivator extends AbstractCyActivator {
    public CyActivator() {
        super();
    }

    public void start(BundleContext bundleContext) {
        CyApplicationManager cyApplicationManagerServiceRef =
                getService(bundleContext, CyApplicationManager.class);
        VisualMappingManager vmmServiceRef = getService(bundleContext, VisualMappingManager.class);

        GEVTaskFactory gEVTaskFactory =
                new GEVTaskFactory(cyApplicationManagerServiceRef, vmmServiceRef);

        Properties gEVTaskFactoryProps = new Properties();
        gEVTaskFactoryProps.setProperty("preferredMenu", "Apps");
        gEVTaskFactoryProps.setProperty("title", "GEV");

        registerService(bundleContext, gEVTaskFactory, TaskFactory.class, gEVTaskFactoryProps);
    }
}