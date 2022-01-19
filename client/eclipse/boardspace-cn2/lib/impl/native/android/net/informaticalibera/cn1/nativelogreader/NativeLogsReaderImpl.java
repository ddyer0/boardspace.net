/**
 * Native Logs Reader
 * Written in 2018 by Francesco Galgani, https://www.informatica-libera.net/
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software. If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package net.informaticalibera.cn1.nativelogreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NativeLogsReaderImpl {

    public void clearAndRestartLog() {
        // https://developer.android.com/studio/command-line/logcat
        try {
            Runtime.getRuntime().exec("logcat -b all -c");
            Runtime.getRuntime().exec("logcat");
        } catch (IOException ex) {
            // logcat non available?
        }
    }

    // more info: https://stackoverflow.com/q/12692103
    public String readLog() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
                log.append("\n");
            }
            return log.toString();
        } catch (IOException e) {
            return "Log is not available.";
        }
    }

    public boolean isSupported() {
        return true;
    }

}
