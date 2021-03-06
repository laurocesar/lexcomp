package br.gov.camara.quadrocomparativo.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;

import br.gov.camara.quadrocomparativo.model.Coluna;
import br.gov.camara.quadrocomparativo.model.QuadroComparativo;

public class QuadroComparativoController {

    private static final Logger log = Logger.getLogger(QuadroComparativoController.class.getName());

    static QuadroComparativo getQuadroComparativo(HttpServletRequest request, String id) {
        QuadroComparativo quadro = null; //SessionController.get(request, id, QuadroComparativo.class);

        if (quadro == null) {
            File f = new File(QuadroComparativo.getFileName(id));

            if (f.exists()) {
                quadro = getQuadroComparativoFromFile(f);
            }

            /*
            if (quadro != null) {
                SessionController.save(request, id, quadro);
            }
            */
        }

//    	if(quadro == null) {
//    		Enumeration<String> e = request.getAttributeNames();
//    		StringBuilder sb = new StringBuilder();
//    		while(e.hasMoreElements()) {
//    			sb.append(", " + e.nextElement());
//    		}
//    	}

        return quadro;
    }

    static QuadroComparativo getQuadroComparativoFromFile(File file) {

        if (file.exists()) {
            try {

                byte[] bs = FileUtils.readFileToByteArray(file);

                JAXBContext jc = JAXBContext.newInstance(QuadroComparativo.class);
                Unmarshaller u = jc.createUnmarshaller();

                return (QuadroComparativo) u.unmarshal(new ByteArrayInputStream(bs));
            } catch (JAXBException ex) {
                log.log(Level.SEVERE, null, ex);
            } catch (IOException e) {
                log.log(Level.SEVERE, "IOException ...", e);
            }
        }

        return null;
    }

    static QuadroComparativo createQuadroComparativo(HttpServletRequest request) {
        Coluna col = new Coluna(null, "Nova Coluna");
        QuadroComparativo quadro = new QuadroComparativo();
        quadro.setTitulo("Novo quadro");
        quadro.addColuna(col);

        saveQuadroToFile(quadro);
        //SessionController.save(request, quadro.getId(), quadro);

        return quadro;
    }

    static boolean deleteQuadroComparativo(HttpServletRequest request, String qcId) {
        File file = new File(QuadroComparativo.getFileName(qcId));
        if (file.delete()) {

            //SessionController.delete(request, qcId, QuadroComparativo.class);

            return true;
        }
        return false;
    }

    static boolean saveQuadroComparativo(HttpServletRequest request, QuadroComparativo qc) {
        QuadroComparativo qcAtualArquivo = getQuadroComparativo(request, qc.getId());

        // garante que o current position é o valor máximo entre o que veio do JS e o que está na sessão
        if (qcAtualArquivo != null) {
            qc.setCurrentPosition(Math.max(qc.getCurrentPosition(), qcAtualArquivo.getCurrentPosition()));
        } else {
            qc.setCurrentPosition(Math.max(qc.getCurrentPosition(), 1));
        }


        if (qc.isArticulacoesExcluidas()) {
            if (!qc.restauraArticulacoes(qcAtualArquivo)){
            	return false;
            }
        }
        
        qc.limpaCorrelacoesSemConexao();

        if (!qc.isArticulacoesExcluidas()) {
            //SessionController.save(request, qc.getId(), qc);
            return saveQuadroToFile(qc);
        }

        return false;
    }

    

    private static boolean saveQuadroToFile(QuadroComparativo quadro) {
        try {

            File quadroFile = new File(quadro.getFileName());
            
            if (!quadroFile.getParentFile().exists()) {
                quadroFile.getParentFile().mkdirs();
            
            } else if (quadroFile.exists()) {
            
                quadroFile.delete();
            }
            
            JAXBContext context = JAXBContext.newInstance(
                    QuadroComparativo.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(quadro, new FileOutputStream(quadroFile));

            return true;

        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            log.log(Level.SEVERE, null, ex);
        }

        return false;
    }
}
