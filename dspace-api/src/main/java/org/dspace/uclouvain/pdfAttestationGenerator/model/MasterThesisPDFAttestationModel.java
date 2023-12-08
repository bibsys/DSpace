package org.dspace.uclouvain.pdfAttestationGenerator.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;


/** 
* Main model used for generating an input data file in order to feed a template.
*/
@XStreamAlias("data")
public class MasterThesisPDFAttestationModel {

    
    public String today = LocalDate
        .now()
        .format(
            DateTimeFormatter.ofPattern(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("uclouvain.pdf_attestation.date_format"))
        );
    public List<Author> authors = new ArrayList<Author>();
    public String title;
    public List<Advisor> advisors = new ArrayList<Advisor>();
    public List<Program> programs = new ArrayList<Program>();
    public String submitter;
    public String handle;
    public List<File> files = new ArrayList<File>();
    @XStreamAlias("abstract")
    public String abstractText;
    public String imagePath;

    
    public String getRenderedXML(){
        XStream xStream = new XStream();
        xStream.processAnnotations(this.getClass());
        return xStream.toXML(this);
    }

    public void addAuthor(String authorName) {
        this.authors.add(new Author(authorName));
    }

    public void addProgram(String programName) {
        this.programs.add(new Program(programName));
    }

    public void addAdvisor(String advisorName) {
        this.advisors.add(new Advisor(advisorName));
    }

    public void addFile(String fileName, String restriction) {
        this.files.add(new File(fileName, restriction));
    }

    @XStreamAlias("author")
    public class Author {
        public String name;

        public Author(String authorName) {
            this.name = authorName;
        }
    }

    @XStreamAlias("advisor")
    public class Advisor {
        public String name;

        public Advisor(String advisorName) {
            this.name = advisorName;
        }
    }

    @XStreamAlias("program")
    public class Program {
        public String name;

        public Program(String programName) {
            this.name = programName;
        }
    }

    @XStreamAlias("file")
    public class File {

        public String name;
        public String accessRestriction;

        public File(String fileName, String restriction) {
            this.name = fileName;
            this.accessRestriction = restriction;
        }
    }
}

