package org.jetuml;

/**
 * Required for packaging to jar, see: https://stackoverflow.com/a/57691362 Just delegates call to
 * actual main, which extends javafx.
 */
public class ShadeJetUML {
  public static void main(String[] args) {
    JetUML.main(args);
  }
}