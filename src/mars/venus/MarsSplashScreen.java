package mars.venus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;
import mars.Main;

/*
 Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Permission is hereby granted, free of charge, to any person obtaining 
 a copy of this software and associated documentation files (the 
 "Software"), to deal in the Software without restriction, including 
 without limitation the rights to use, copy, modify, merge, publish, 
 distribute, sublicense, and/or sell copies of the Software, and to 
 permit persons to whom the Software is furnished to do so, subject 
 to the following conditions:

 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 (MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Produces MARS splash screen.
 * 
 * @author Andrea Proietto
 */
public class MarsSplashScreen extends JWindow {

    private MarsSplashScreen() {

        // Construct the labels
        JLabel title = new JLabel("MARS: Mips Assembler and Runtime Simulator", JLabel.CENTER);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        title.setForeground(Color.black);
        
        JLabel copyright = new JLabel("<html><br/><br/>Version " + Main.VERSION
                + " Copyright (c) " + Main.COPYRIGHT_YEARS
                + "<br/><br/><br/>" + Main.COPYRIGHT_HOLDERS + "</html>",
                JLabel.CENTER);
        copyright.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        copyright.setForeground(Color.white);

        // Construct the panel with the background image
        JPanel content = new JPanel() {
            ImageIcon image = new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                    this.getClass().getResource(Main.imagesPath + "MarsSurfacePathfinder.jpg")));

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image.getImage(), 0, 0, this);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(image.getIconWidth(), image.getIconHeight());
            }
        };

        // Compose and finalize the splash
        content.add(title);
        content.add(copyright);
        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Show the application's splash screen.
     *
     * @param duration The time expressed in milliseconds to display the splash
     */
    public static void showSplash(int duration) {
        MarsSplashScreen splash = new MarsSplashScreen();
        splash.setVisible(true);

        new Timer(duration, (event) -> splash.setVisible(false)).start();
    }
}
