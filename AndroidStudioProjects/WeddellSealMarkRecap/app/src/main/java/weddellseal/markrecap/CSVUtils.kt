package weddellseal.markrecap

import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


public class CSVUtils {
    fun writeDataAtOnce(file: File, observations: MutableList<Array<String>>) {

        // first create file object for file placed at location
        // specified by filepath
        try {
            // create FileWriter object with file as parameter
            val outputfile = FileWriter(file)

            // create CSVWriter object filewriter object as parameter
            val writer = CSVWriter(outputfile)

            // create a List which contains String array
            writer.writeAll(observations)

            // closing writer connection
            writer.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }



}