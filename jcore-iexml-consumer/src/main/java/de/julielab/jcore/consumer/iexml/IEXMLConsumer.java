/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.consumer.iexml;

import generated.AnnoType;
import generated.Lang;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.mantra.Corpus;
import de.julielab.jcore.types.mantra.Document;
import de.julielab.jcore.types.mantra.Entity;
import de.julielab.jcore.types.mantra.NER;
import de.julielab.jcore.types.mantra.Unit;

public class IEXMLConsumer extends CasConsumer_ImplBase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(IEXMLConsumer.class);

	public static final String PARAM_OUTPUTFILE = "outputFile";

	private static final String PARAM_AUTHOR = "author";

	private static final String PARAM_DESCRIPTION = "description";

	private static final String WRITE_NER = "writeNER";

	private String outputFile;

	private String author;

	private String description;

	private boolean writeNER;

	private static generated.Corpus xmlCorpus = null;

	@Override
	public void initialize() throws ResourceInitializationException {
		// get the parameters
		this.outputFile = (String) getConfigParameterValue(PARAM_OUTPUTFILE);
		this.author = (String) getConfigParameterValue(PARAM_AUTHOR);
		this.description = (String) getConfigParameterValue(PARAM_DESCRIPTION);
		Object x = getConfigParameterValue(WRITE_NER);
		this.writeNER =  x != null && (Boolean) x;
	}

	@Override
	public void processCas(CAS cas) throws ResourceProcessException {
		JCas jcas = null;
		try {
			jcas = cas.getJCas();
		} catch (CASException e) {
			e.printStackTrace();
		}
		String docText = jcas.getDocumentText();

		if (xmlCorpus == null)
			setXMLCorpusAttributes(jcas);

		// Document
		Document doc = (Document) jcas.getAnnotationIndex(Document.type)
				.iterator().next();
		generated.Document xmlDoc = new generated.Document();
		xmlDoc.setId(doc.getId());

		// Units
		FSIterator<Annotation> units = jcas.getAnnotationIndex(Unit.type)
				.iterator();
		while (units.hasNext()) {
			Unit unit = (Unit) units.next();
			generated.Unit xmlUnit = new generated.Unit();
			int offset = unit.getBegin();

			xmlUnit.setId(unit.getId());

			generated.Unit.Text xmlText = new generated.Unit.Text();
			xmlText.getContent().add(
					docText.substring(unit.getBegin(), unit.getEnd()));
			xmlUnit.setText(xmlText);

			FSIterator<Annotation> entities = jcas.getAnnotationIndex(
					Entity.type).subiterator(unit);
			int numberOfEntities = 1;
			String idTemplate = unit.getId() + ".e%s";
			while (entities.hasNext()) {
				Entity e = (Entity) entities.next();
				generated.E xmlE = new generated.E();

				xmlE.setOffset(BigInteger.valueOf(e.getBegin() - offset));
				xmlE.setLen(BigInteger.valueOf(e.getEnd() - e.getBegin()));

				xmlE.setId(String.format(idTemplate, numberOfEntities));

				if (!writeNER) {
					xmlE.setSrc(e.getSource());
					xmlE.setCui(e.getCui());
					xmlE.setType(e.getSemanticType());
					xmlE.setGrp(generated.Group.fromValue(e.getSemanticGroup()));
				}
				
				List<Serializable> content = xmlE.getContent();
				content.add(e.getCoveredText());

				if (writeNER) {
					FSArray ners = e.getNer();
					for (int i = 0; i < ners.size(); ++i) {
						generated.NER xmlNER = new generated.NER();
						NER ner = (NER) ners.get(i);
						xmlNER.setGroup(generated.Group.fromValue(ner
								.getSemanticGroup()));
						xmlNER.setProbability(ner.getProbability());
						content.add((Serializable) xmlNER);// why do i have to
															// cast
															// it? was generated
															// by
															// jaxb...
					}
				}

				numberOfEntities++;
				xmlUnit.getE().add(xmlE);
			}

			// TODO handle w here

			xmlDoc.getUnit().add(xmlUnit);
		}
		xmlCorpus.getDocument().add(xmlDoc);
	}

	private void setXMLCorpusAttributes(JCas jcas) {
		Corpus corpus = (Corpus) jcas.getAnnotationIndex(Corpus.type)
				.iterator().next();
		xmlCorpus = new generated.Corpus();

		// attributes
		xmlCorpus.setAnnotationType(AnnoType.STANDOFF);
		xmlCorpus.setAuthor(author);
		xmlCorpus.setDescription(description);
		xmlCorpus.setDocType(corpus.getDocType());
		xmlCorpus.setLang(Lang.fromValue(corpus.getLanguage()));

		String id = outputFile.substring(
				outputFile.lastIndexOf(File.separator) + 1,
				outputFile.lastIndexOf("."));
		xmlCorpus.setId(id);

		// finally the dates
		try {
			GregorianCalendar c = new GregorianCalendar();
			de.julielab.jcore.types.Date creationDate = corpus
					.getCreationDate();
			// Stupid inconsistent API: The first month of the year is JANUARY
			// which is 0
			c.set(creationDate.getYear(), creationDate.getMonth() - 1,
					creationDate.getDay());
			XMLGregorianCalendar xmlCreationDate = DatatypeFactory
					.newInstance().newXMLGregorianCalendar(c);
			xmlCreationDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			xmlCorpus.setCreationDate(xmlCreationDate);

			GregorianCalendar d = new GregorianCalendar();
			d.setGregorianChange(new Date());
			XMLGregorianCalendar xmlAnnotationDate = DatatypeFactory
					.newInstance().newXMLGregorianCalendar(d);
			xmlAnnotationDate.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			xmlCorpus.setAnnotationDate(xmlAnnotationDate);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {

		LOGGER.info("Writing altered content back to " + outputFile + ".");
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance("generated");
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
					Boolean.TRUE);
			marshaller.marshal(xmlCorpus, new File(outputFile));
		} catch (JAXBException e) {
			LOGGER.error("Something got wrong while trying to write content to "
					+ outputFile + ".");
		}

		super.destroy();
	}

}
