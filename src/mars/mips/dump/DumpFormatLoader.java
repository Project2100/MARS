package mars.mips.dump;

import mars.util.*;
import java.util.*;
import java.lang.reflect.*;

/*
 Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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

/* This class provides functionality to bring external memory dump format definitions
 * into MARS.  This is adapted from the ToolLoader class, which is in turn adapted
 * from Bret Barker's GameServer class from the book "Developing Games In Java".
 */
public class DumpFormatLoader {

    private final String CLASS_PREFIX = "mars.mips.dump.";
    private final String DUMP_DIRECTORY_PATH = "mars/mips/dump";
    private final String SYSCALL_INTERFACE = "DumpFormat.class";
    private final String CLASS_EXTENSION = "class";

    private static ArrayList<DumpFormat> formatList = null;

    /**
     * Dynamically loads dump formats into an ArrayList. This method is adapted
     * from the loadGameControllers() method in Bret Barker's GameServer class.
     * Barker (bret@hypefiend.com) is co-author of the book "Developing Games in
     * Java". Also see the ToolLoader and SyscallLoader classes elsewhere in
     * MARS.
     *
     * @return
     */
    public ArrayList<DumpFormat> loadDumpFormats(){
        // The list will be populated only the first time this method is called.
        if (formatList == null) {
            formatList = new ArrayList<>();
            // grab all class files in the dump directory
            ArrayList<String> candidates = FilenameFinder.getFilenameList(this.getClass().getClassLoader(),
                    DUMP_DIRECTORY_PATH, CLASS_EXTENSION);

            for (String filename : candidates)
                try {
                    // grab the class, make sure it implements DumpFormat, instantiate, add to list
                    Class clas = Class.forName(CLASS_PREFIX + filename.substring(0, filename.indexOf(CLASS_EXTENSION) - 1));
                    
                    if (DumpFormat.class.isAssignableFrom(clas)
                            && !clas.isInterface()
                            && !Modifier.isAbstract(clas.getModifiers()))
                        formatList.add(   (DumpFormat) clas.newInstance());
                }
                catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    System.out.println("Error instantiating DumpFormat from file " + filename + ": " + e);
                }
        }
        return formatList;
    }

    public DumpFormat[] getDumpFormatsArray() {
        DumpFormat[] d = new DumpFormat[formatList.size()];
        Iterator<DumpFormat> itr = formatList.iterator();
        for (int i = 0; i < d.length; i++)
            d[i] = itr.next();
        return d;
    }

    public static DumpFormat fromCommandDescriptor(String targetDescriptor) {
        for (DumpFormat format : formatList)
            if (format.getCommandDescriptor().equals(targetDescriptor))
                return format;
        return null;
    }

}
