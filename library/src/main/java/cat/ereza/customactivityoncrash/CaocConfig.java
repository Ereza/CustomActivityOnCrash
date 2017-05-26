/*
 * Copyright 2014-2017 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cat.ereza.customactivityoncrash;

import android.app.Activity;
import android.support.annotation.DrawableRes;

import java.io.Serializable;
import java.lang.reflect.Modifier;

public class CaocConfig implements Serializable {
    private boolean launchWhenInBackground = false;
    private boolean showErrorDetails = true;
    private boolean showRestartButton = true;
    private boolean trackActivities = false;
    private Integer errorDrawable = null;
    private Class<? extends Activity> errorActivityClass = null;
    private Class<? extends Activity> restartActivityClass = null;
    private CustomActivityOnCrash.EventListener eventListener = null;

    public boolean isLaunchWhenInBackground() {
        return launchWhenInBackground;
    }

    public void setLaunchWhenInBackground(boolean launchWhenInBackground) {
        this.launchWhenInBackground = launchWhenInBackground;
    }

    public boolean isShowErrorDetails() {
        return showErrorDetails;
    }

    public void setShowErrorDetails(boolean showErrorDetails) {
        this.showErrorDetails = showErrorDetails;
    }

    public boolean isShowRestartButton() {
        return showRestartButton;
    }

    public void setShowRestartButton(boolean showRestartButton) {
        this.showRestartButton = showRestartButton;
    }

    public boolean isTrackActivities() {
        return trackActivities;
    }

    public void setTrackActivities(boolean trackActivities) {
        this.trackActivities = trackActivities;
    }

    public Integer getErrorDrawable() {
        return errorDrawable;
    }

    public void setErrorDrawable(Integer errorDrawable) {
        this.errorDrawable = errorDrawable;
    }

    public Class<? extends Activity> getErrorActivityClass() {
        return errorActivityClass;
    }

    public void setErrorActivityClass(Class<? extends Activity> errorActivityClass) {
        this.errorActivityClass = errorActivityClass;
    }

    public Class<? extends Activity> getRestartActivityClass() {
        return restartActivityClass;
    }

    public void setRestartActivityClass(Class<? extends Activity> restartActivityClass) {
        this.restartActivityClass = restartActivityClass;
    }

    public CustomActivityOnCrash.EventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(CustomActivityOnCrash.EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public static class Builder {
        private CaocConfig config;

        public static Builder create() {
            Builder builder = new Builder();
            CaocConfig currentConfig = CustomActivityOnCrash.getConfig();

            CaocConfig config = new CaocConfig();

            if (currentConfig != null) {
                config.launchWhenInBackground = currentConfig.launchWhenInBackground;
                config.showErrorDetails = currentConfig.showErrorDetails;
                config.showRestartButton = currentConfig.showRestartButton;
                config.trackActivities = currentConfig.trackActivities;
                config.errorDrawable = currentConfig.errorDrawable;
                config.errorActivityClass = currentConfig.errorActivityClass;
                config.restartActivityClass = currentConfig.restartActivityClass;
                config.eventListener = currentConfig.eventListener;
            }

            builder.config = config;

            return builder;
        }

        /**
         * Defines if the error activity must be launched when the app is on background.
         * Set it to true if you want to launch the error activity when the app is in background,
         * false if you want it not to launch and crash silently.
         * This has no effect in API<14 and the error activity is always launched.
         * The default is true (the app will be brought to front when a crash occurs).
         */
        public Builder launchWhenInBackground(boolean launchWhenInBackground) {
            config.launchWhenInBackground = launchWhenInBackground;
            return this;
        }

        /**
         * Defines if the error activity must shown the error details button.
         * Set it to true if you want to show the full stack trace and device info,
         * false if you want it to be hidden.
         * The default is true.
         */
        public Builder showErrorDetails(boolean showErrorDetails) {
            config.showErrorDetails = showErrorDetails;
            return this;
        }

        /**
         * Defines if the error activity should show a restart button.
         * Set it to true if you want to show a restart button,
         * false if you want to show a close button.
         * Note that even if restart is enabled but you app does not have any launcher activities,
         * a close button will still be used by the default error activity.
         * The default is true.
         */
        public Builder showRestartButton(boolean showRestartButton) {
            config.showRestartButton = showRestartButton;
            return this;
        }

        /**
         * Defines if the activities visited by the user should be tracked
         * so they are reported when an error occurs.
         * The default is false.
         */
        public Builder trackActivities(boolean trackActivities) {
            config.trackActivities = trackActivities;
            return this;
        }

        /**
         * Defines which drawable to use in the default error activity image.
         * Set this if you want to use an image other than the default one.
         * The default is R.drawable.customactivityoncrash_error_image (a cute upside-down bug).
         */
        public Builder errorDrawable(@DrawableRes Integer errorDrawable) {
            config.errorDrawable = errorDrawable;
            return this;
        }

        /**
         * Sets the error activity class to launch when a crash occurs.
         * If null, the default error activity will be used.
         */
        public Builder errorActivity(Class<? extends Activity> errorActivityClass) {
            config.errorActivityClass = errorActivityClass;
            return this;
        }

        /**
         * Sets the main activity class that the error activity must launch when a crash occurs.
         * If not set or set to null, the default launch activity will be used.
         * If your app has no launch activities and this is not set, the default error activity will close instead.
         */
        public Builder restartActivity(Class<? extends Activity> restartActivityClass) {
            config.restartActivityClass = restartActivityClass;
            return this;
        }

        /**
         * Sets an event listener to be called when events occur, so they can be reported
         * by the app as, for example, Google Analytics events.
         * If not set or set to null, no events will be reported.
         *
         * @param eventListener The event listener.
         * @throws IllegalArgumentException if the eventListener is an inner or anonymous class
         */
        public Builder eventListener(CustomActivityOnCrash.EventListener eventListener) {
            if (eventListener != null && eventListener.getClass().getEnclosingClass() != null && !Modifier.isStatic(eventListener.getClass().getModifiers())) {
                throw new IllegalArgumentException("The event listener cannot be an inner or anonymous class, because it will need to be serialized. Change it to a class of its own, or make it a static inner class.");
            } else {
                config.eventListener = eventListener;
            }
            return this;
        }

        public CaocConfig get() {
            return config;
        }

        public void apply() {
            CustomActivityOnCrash.setConfig(config);
        }
    }


}
