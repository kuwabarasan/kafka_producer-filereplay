package hiro.kafka.FileFilters;

import java.io.File;
import java.io.FileFilter;

public class JsonOrDirFilter implements FileFilter {

    @Override
    public boolean accept(File pathname){
        if (pathname.isHidden())
            return false;

        boolean isItJson;
        isItJson = pathname.toString().toLowerCase().endsWith(".json") ? true : false;

        boolean isItDirectory;
        isItDirectory = pathname.isDirectory();

        return (isItJson || isItDirectory);
    }

}