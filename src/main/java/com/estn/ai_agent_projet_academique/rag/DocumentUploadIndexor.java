package com.estn.ai_agent_projet_academique.rag;


import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Component
public class DocumentUploadIndexor {

    @Value("${app.store.filename:store.json}")
    private String filestore;

    @Autowired
    private SimpleVectorStore vectorStore;

    public void loadFile(MultipartFile pdfFile) throws Exception {
        Path path = Path.of("src", "main", "resources", "store");
        File file = new File(path.toFile(), filestore);

        if (file.exists()) {
            vectorStore.load(file);
        } else {
            List<Document> documents;
            String name = pdfFile.getOriginalFilename();

            if (name != null && name.toLowerCase().endsWith(".pdf")) {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfFile.getResource());
                documents = pdfReader.get();
            } else {
                TikaDocumentReader tikaReader = new TikaDocumentReader(pdfFile.getResource());
                documents = tikaReader.get();
            }

            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.apply(documents);
            vectorStore.add(chunks);
            path.toFile().mkdirs();
            vectorStore.save(file);
        }
    }
}
