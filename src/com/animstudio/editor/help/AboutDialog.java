package com.animstudio.editor.help;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * About dialog showing application information.
 */
public class AboutDialog extends Dialog<Void> {
    
    private static final String APP_NAME = "AnimStudio";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_DESCRIPTION = "Professional 2D Skeletal Animation Software";
    private static final String COPYRIGHT = "© 2025 AnimStudio Team";
    
    public AboutDialog() {
        setTitle("About " + APP_NAME);
        setHeaderText(null);
        
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(30));
        
        // Logo placeholder (could be replaced with actual logo)
        Label logoLabel = new Label("🎬");
        logoLabel.setStyle("-fx-font-size: 64px;");
        
        // Application name
        Label nameLabel = new Label(APP_NAME);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        
        // Version
        Label versionLabel = new Label("Version " + APP_VERSION);
        versionLabel.setFont(Font.font("System", 14));
        
        // Description
        Label descLabel = new Label(APP_DESCRIPTION);
        descLabel.setFont(Font.font("System", 12));
        
        // Separator
        Separator separator = new Separator();
        
        // Features section
        VBox featuresBox = new VBox(5);
        featuresBox.setAlignment(Pos.CENTER_LEFT);
        
        Label featuresTitle = new Label("Features:");
        featuresTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        Label features = new Label("""
            • Skeletal animation with bone hierarchy
            • Forward and Inverse Kinematics (IK)
            • Mesh deformation with skinning
            • Timeline editor with keyframe support
            • Multiple easing functions
            • Export to PNG, GIF, Video, and Sprite Sheets
            • Project save/load in JSON format
            """);
        features.setFont(Font.font("System", 11));
        
        featuresBox.getChildren().addAll(featuresTitle, features);
        
        // System info
        VBox systemBox = new VBox(3);
        systemBox.setAlignment(Pos.CENTER_LEFT);
        
        Label systemTitle = new Label("System Information:");
        systemTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        
        Label systemInfo = new Label(String.format("""
            Java Version: %s
            JavaFX Version: %s
            OS: %s %s
            Architecture: %s
            """,
            System.getProperty("java.version"),
            System.getProperty("javafx.version", "Unknown"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")));
        systemInfo.setFont(Font.font("System", 11));
        
        systemBox.getChildren().addAll(systemTitle, systemInfo);
        
        // Copyright
        Label copyrightLabel = new Label(COPYRIGHT);
        copyrightLabel.setFont(Font.font("System", 10));
        copyrightLabel.setStyle("-fx-text-fill: gray;");
        
        // Links
        HBox linksBox = new HBox(15);
        linksBox.setAlignment(Pos.CENTER);
        
        Hyperlink websiteLink = new Hyperlink("Website");
        websiteLink.setOnAction(e -> openUrl("https://github.com/animstudio"));
        
        Hyperlink licenseLink = new Hyperlink("License");
        licenseLink.setOnAction(e -> showLicenseDialog());
        
        Hyperlink creditsLink = new Hyperlink("Credits");
        creditsLink.setOnAction(e -> showCreditsDialog());
        
        linksBox.getChildren().addAll(websiteLink, licenseLink, creditsLink);
        
        content.getChildren().addAll(
            logoLabel,
            nameLabel,
            versionLabel,
            descLabel,
            separator,
            featuresBox,
            systemBox,
            copyrightLabel,
            linksBox
        );
        
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(400);
    }
    
    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private void showLicenseDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("License");
        alert.setHeaderText("MIT License");
        alert.setContentText("""
            Copyright (c) 2025 AnimStudio Team
            
            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:
            
            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.
            
            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
            """);
        alert.showAndWait();
    }
    
    private void showCreditsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Credits");
        alert.setHeaderText("Credits & Acknowledgments");
        alert.setContentText("""
            Development Team:
            • Lead Developer
            • UI/UX Designer
            • Animation Specialist
            
            Libraries Used:
            • JavaFX - UI Framework
            • Jackson - JSON Processing
            • JCodec - Video Encoding
            
            Special Thanks:
            • The open-source community
            • Beta testers and contributors
            """);
        alert.showAndWait();
    }
    
    public static String getAppName() {
        return APP_NAME;
    }
    
    public static String getAppVersion() {
        return APP_VERSION;
    }
}
