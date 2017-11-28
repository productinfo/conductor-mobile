package com.joss.conductor.mobile;

import org.pmw.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class ConductorConfig {
    private static final String DefaultConfigFile = "/config.yaml";

    // Conductor properties
    private String[] currentSchemes;
    private int timeout = 5;
    private int retries = 5;
    private boolean screenshotOnFail = true;

    // Appium Properties
    private Platform platformName = Platform.NONE;
    private String deviceName;
    private boolean autoGrantPermissions = true;
    private boolean noReset = true;
    private boolean fullReset = false;
    private String appiumVersion;
    private String platformVersion;
    private String appFile;
    private String language;
    private String locale;
    private String orientation;
    private String hub;
    private String udid;
    private String automationName;
    private String appPackageName;

    // iOS specific
    private String xcodeSigningId;
    private String xcodeOrgId;

    // Android specific
    private String avd;
    private String appActivity;
    private String appWaitActivity;
    private String intentCategory;

    public ConductorConfig() {
        try {
            InputStream is = this.getClass().getResourceAsStream(DefaultConfigFile);
            if (is != null) {
                readConfig(is);
            }
        } catch (Exception e) {
            Logger.error("Couldn't load default conductor config! ", e);
        }
    }

    public ConductorConfig(InputStream yamlStream) {
        readConfig(yamlStream);
    }

    public Platform getPlatformName() {
        return platformName;
    }

    public String[] getCurrentSchemes() {
        return currentSchemes;
    }

    public String getFullAppPath() {
        if (appFile == null) {
            return null;
        }

        Path path = Paths.get(appFile);
        if (appFile.startsWith("sauce-storage:") || path.isAbsolute()) {
            return appFile;
        }
        try {
            URL url = new URL(appFile);
            return appFile;
        } catch(MalformedURLException e) {
            // Ignore, this is expected to happen, parse as a regular path
        }

        return Paths.get(System.getProperty("user.dir"), path.normalize().toString()).normalize().toString();
    }

    private void readConfig(InputStream is) {
        if(is == null) {
            throw new NullPointerException("InputStream parameter to readConfig cannot be null");
        }

        Yaml yaml = new Yaml();
        Map<String, Object> config = (Map<String, Object>) yaml.load(is);

        String environmentPlatformName = System.getProperty("conductorPlatformName");
        if (environmentPlatformName != null) {
            platformName = Platform.valueOf(environmentPlatformName);
        } else if (config.containsKey("platformName")) {
            platformName = Platform.valueOf((String) config.get("platformName"));
        }

        Map<String, Object> defaults = (Map<String, Object>) config.get("defaults");
        if (defaults != null) {
            readProperties(defaults);

            Map<String, Object> platformDefaults = null;
            switch (platformName) {
                case IOS:
                    platformDefaults = (Map<String, Object>) defaults.get("ios");
                    break;
                case ANDROID:
                    platformDefaults = (Map<String, Object>) defaults.get("android");
                    break;
            }
            if (platformDefaults != null) {
                readProperties(platformDefaults);
            }
        }

        String environmentSchemes = System.getProperty("conductorCurrentSchemes");
        if (environmentSchemes != null) {
            currentSchemes = environmentSchemes.split(",");
        } else {
            List<String> schemesList = (List<String>) config.get("currentSchemes");
            if (schemesList != null) {
                currentSchemes = new String[schemesList.size()];
                schemesList.toArray(currentSchemes);
            }
        }

        if (currentSchemes != null) {

            for (String scheme : currentSchemes) {
                Map<String, Object> schemeData = (Map<String, Object>) config.get(scheme);
                if (schemeData != null) {
                    readProperties(schemeData);
                }
            }
        }
    }

    private void readProperties(Map<String, Object> properties) {
        for (String key : properties.keySet()) {
            if (key.equals("ios") || key.equals("android")) {
                // These keys are the start of platform specific options
                // and should be ignored
                continue;
            }

            setProperty(key, (String) properties.get(key).toString());
        }
    }

    private void setProperty(String propertyName, String propertyValue) {
        Method[] methods = this.getClass().getMethods();

        String capPropertyKey = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String methodName = "set" + capPropertyKey;
        Method foundMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                foundMethod = method;
                break;
            }
        }

        if (foundMethod != null) {
            try {
                if (foundMethod.getParameterTypes()[0] == String.class) {
                    foundMethod.invoke(this, propertyValue);
                } else if (foundMethod.getParameterTypes()[0] == boolean.class) {
                    boolean value = Boolean.parseBoolean(propertyValue);
                    foundMethod.invoke(this, value);
                } else if (foundMethod.getParameterTypes()[0] == int.class) {
                    int value = Integer.parseInt(propertyValue);
                    foundMethod.invoke(this, value);
                } else if (foundMethod.getParameterTypes()[0] == Platform.class) {
                    Platform value = Platform.valueOf(propertyValue);
                    foundMethod.invoke(this, value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                Logger.warn(e, "Could not invoke method: $s", methodName);
            }
        }
    }

    public boolean getNoReset() {
        return noReset;
    }

    public void setNoReset(boolean noReset) {
        this.noReset = noReset;
    }

    public String getAppiumVersion() {
        return appiumVersion;
    }

    public void setAppiumVersion(String appiumVersion) {
        this.appiumVersion = appiumVersion;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isFullReset() {
        return fullReset;
    }

    public void setFullReset(boolean fullReset) {
        this.fullReset = fullReset;
    }

    public String getAppFile() {
        return appFile;
    }

    public void setAppFile(String appFile) {
        this.appFile = appFile;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public URL getHub() {
        URL url = null;
        if (hub != null) {
            try {
                url = new URL(hub);
            } catch (MalformedURLException e) {
                Logger.error("Failure parsing Hub Url", e);
            }
        }

        return url;
    }

    public void setHub(String hub) {
        this.hub = hub;
    }

    public String getUdid() {
        return udid;
    }

    public void setUdid(String udid) {
        this.udid = udid;
    }

    public String getAutomationName() {
        return automationName;
    }

    public void setAutomationName(String automationName) {
        this.automationName = automationName;
    }

    public String getXcodeSigningId() {
        return xcodeSigningId;
    }

    public void setXcodeSigningId(String xcodeSigningId) {
        this.xcodeSigningId = xcodeSigningId;
    }

    public String getXcodeOrgId() {
        return xcodeOrgId;
    }

    public void setXcodeOrgId(String xcodeOrgId) {
        this.xcodeOrgId = xcodeOrgId;
    }

    public String getAppActivity() {
        return appActivity;
    }

    public void setAppActivity(String appActivity) {
        this.appActivity = appActivity;
    }

    public String getAppWaitActivity() {
        return appWaitActivity;
    }

    public void setAppWaitActivity(String appWaitActivity) {
        this.appWaitActivity = appWaitActivity;
    }

    public String getIntentCategory() {
        return intentCategory;
    }

    public void setIntentCategory(String intentCategory) {
        this.intentCategory = intentCategory;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    public boolean isScreenshotOnFail() {
        return screenshotOnFail;
    }

    public void setScreenshotOnFail(boolean screenshotOnFail) {
        this.screenshotOnFail = screenshotOnFail;
    }

    public String getAvd() {
        return avd;
    }

    public void setAvd(String avd) {
        this.avd = avd;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isAutoGrantPermissions() {
        return autoGrantPermissions;
    }

    public void setAutoGrantPermissions(boolean autoGrantPermissions) {
        this.autoGrantPermissions = autoGrantPermissions;
    }
}