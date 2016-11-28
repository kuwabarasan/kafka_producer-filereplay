package hiro.kafka.FileFilters;


import java.io.File;
import java.io.FileFilter;

public class SimpleFilter implements FileFilter {

    @Override
    public boolean accept(File pathname){
        return !pathname.isHidden();
    }
}