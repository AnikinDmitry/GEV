package org.cytoscape.app.internal;

import org.cytoscape.model.*;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GEVTask extends AbstractTask {
    private final CyNetwork net;
    private final CyNetworkView netView;
    private final VisualMappingManager vmmServiceRef;
    private final String[] tissueNames = {"t-cell", "pancreatic", "muscle", "artery", "liver"};

    public GEVTask(CyNetwork net, CyNetworkView netView,
                   VisualMappingManager vmmServiceRef) {
        this.net = net;
        this.netView = netView;
        this.vmmServiceRef = vmmServiceRef;
    }

    public void run(TaskMonitor taskMonitor) {
        List<CyNode> nodes = CyTableUtil.getNodesInState(netView.getModel(),
                CyNetwork.SELECTED, true);
        nodes.addAll(CyTableUtil.getNodesInState(netView.getModel(),
                CyNetwork.SELECTED, false));
 
        JFrame window = new JFrame("Gene Expression Visualizer");
        window.setLocation(100, 100);
        JButton[] tissueButtons = new JButton[tissueNames.length];
        JButton reset = new JButton("reset"), importData = new JButton("import data");
        JPanel buttonsPanel = new JPanel(new FlowLayout());

        int tissueNumber = 0;
        for (String tissueName : tissueNames) {
            tissueButtons[tissueNumber] = new JButton(tissueName);
            tissueButtons[tissueNumber].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (CyNode node : nodes) {
                        netView.getNodeView(node).setLockedValue(BasicVisualLexicon.NODE_WIDTH,
                                (net.getRow(node).get(tissueName, Double.class) / 10 + 1) *
                                        Double.parseDouble(net.getRow(node)
                                                .get("Width", String.class)));
                        netView.getNodeView(node).setLockedValue(BasicVisualLexicon.NODE_HEIGHT,
                                (net.getRow(node).get(tissueName, Double.class) / 10 + 1) *
                                        Double.parseDouble(net.getRow(node)
                                                .get("Height", String.class)));
                    }
                }
            });
            buttonsPanel.add(tissueButtons[tissueNumber]);
            tissueNumber++;
        }

        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (CyNode node : nodes) {
                    netView.getNodeView(node).setLockedValue(BasicVisualLexicon.NODE_WIDTH,
                            Double.parseDouble(net.getRow(node).get("Width", String.class)));
                    netView.getNodeView(node).setLockedValue(BasicVisualLexicon.NODE_HEIGHT,
                            Double.parseDouble(net.getRow(node).get("Height", String.class)));
                }
            }
        });
        buttonsPanel.add(reset);

        importData.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Load data of expression");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setFileFilter(new FileNameExtensionFilter(".csv", "csv"));

                if (fileChooser.showOpenDialog(buttonsPanel) == JFileChooser.APPROVE_OPTION) {
                    File dataFile = fileChooser.getSelectedFile();
                    try {
                        Scanner inputSteam = new Scanner(dataFile);
                        ArrayList<String[]> dataList = new ArrayList<>();
                        while (inputSteam.hasNext()) {
                            String[] data = inputSteam.next().split(";");
                            if (data[0].startsWith("hsa:")) {
                                for (int i = 0; i < data.length; i++) {
                                    if (data[i].equals("") || data[i].equals("Nan"))
                                        data[i] = "0.0";
                                    data[i] = data[i].replace(',', '.');
                                }
                                dataList.add(data);
                            }
                        }

                        for (int i = 0; i < tissueNames.length; i++) {
                            if (net.getDefaultNodeTable().getColumn(tissueNames[i]) != null)
                                net.getDefaultNodeTable().deleteColumn(tissueNames[i]);
                            net.getDefaultNodeTable()
                                    .createColumn(tissueNames[i], Double.class, false);

                            for (CyNode node : nodes) {
                                String[] nodeGeneNames = net.getRow(node)
                                        .get(CyNetwork.NAME, String.class).split(" ");
                                if (!nodeGeneNames[0].startsWith("hsa:"))
                                    net.getRow(node).set(tissueNames[i], 0d);
                                else {
                                    double coef = 0;
                                    int n = 0;
                                    for (String nodeGeneName : nodeGeneNames)
                                        for (String[] data : dataList)
                                            if (data[0].equals(nodeGeneName) &&
                                                    !data[i + 1].equals("0.0")) {
                                                coef += Double.parseDouble(data[i + 1]);
                                                n++;
                                                break;
                                            }
                                    if (n != 0) coef /= n;
                                    net.getRow(node).set(tissueNames[i], coef);
                                }
                            }
                        }

                        inputSteam.close();
                    } catch (FileNotFoundException fileNotFoundException) {
                        fileNotFoundException.printStackTrace();
                    }
                }
            }
        });
        buttonsPanel.add(importData);

        window.add(buttonsPanel);
        window.pack();
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setVisible(true);

        VisualStyle style = vmmServiceRef.getCurrentVisualStyle();
        style.apply(netView);
        netView.updateView();
    }
}