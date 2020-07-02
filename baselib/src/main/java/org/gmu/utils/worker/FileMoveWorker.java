package org.gmu.utils.worker;

import android.util.Log;
import org.gmu.utils.FileDescriptor;
import org.gmu.utils.Utils;

import java.io.File;
import java.util.Stack;

/**
 * User: ttg
 * Date: 25/04/13
 * Time: 15:22
 * To change this template use File | Settings | File Templates.
 */
public class FileMoveWorker extends AbstractWorker
{
    private File destDir;
    private File srcDir;
    private String sourcePrefix;
    private long moved=0;
    public FileMoveWorker(Stack workerQueue,File srcDir,File destDir,String sourcePrefix)
    {
        super(workerQueue);
        this.sourcePrefix=sourcePrefix;
        this.srcDir =srcDir;
        this.destDir=destDir;
    }

    @Override
    protected long doTask(Object work) throws Exception
    {
        FileDescriptor toMove = (FileDescriptor)work;
        String next = toMove.name;

        String relativeUrl = next.replace(sourcePrefix, "");
        File srcFile = new File(srcDir, relativeUrl);
        File target = new File(destDir, relativeUrl);
        synchronized (workerQueue){new File(target.getParent()).mkdirs();}



        boolean result = srcFile.renameTo(target);

        if (!result&&!target.exists())
        {   throw new Exception("File cannot be moved="+srcFile.getName());

        }
        moved+=1;
        return moved;
    }
}
