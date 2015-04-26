/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.venus;

import java.awt.event.ActionEvent;
import java.util.function.BiConsumer;
import javax.swing.Action;
import mars.Main;

/**
 *
 * @author Project2100
 */
public class FileActions {
    
    static BiConsumer<GuiAction, ActionEvent> saveAs= (action, event)->{
        Main.getEnv().editor.saveAs();
    };
    
    public static void saveAll(Action action, ActionEvent event) {
        Main.getEnv().editor.saveAll();
    }
    
    public static void save(ActionEvent e) {
        Main.getEnv().editor.save();
    }

}
