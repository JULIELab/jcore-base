package de.julielab.jcore.reader.cord19;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.julielab.jcore.reader.cord19.jsonformat.Affiliation;
import de.julielab.jcore.reader.cord19.jsonformat.Author;
import de.julielab.jcore.reader.cord19.jsonformat.Cord19Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonFormatTest {
    @Test
    public void readJsonDocument() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Cord19Document document = om.readValue(Path.of("src", "test", "resources", "documents", "99408604499bba576bd955c922a371c5d35bc969.json").toFile(), Cord19Document.class);
        assertThat(document).isNotNull();
        assertThat(document.getMetadata().getAuthors()).hasSize(7);
        Author secondAuthor = document.getMetadata().getAuthors().get(1);
        assertThat(secondAuthor).extracting(Author::getFirst).isEqualTo("Giuseppe");
        assertThat(secondAuthor).extracting(Author::getLast).isEqualTo("Fiermonte");
        assertThat(secondAuthor).extracting(Author::getAffiliation)
                .isNotNull()
                .extracting("laboratory", "institution").containsExactly("Laboratory of Biochemistry and Molecular Biology", "University of Bari");
        assertThat(secondAuthor.getAffiliation()).extracting(Affiliation::getLocation).extracting("settlement", "country").containsExactly("Bari", "Italy");
    }

    @Test
    public void readJsonDocument2() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Cord19Document document = om.readValue(Path.of("src", "test", "resources", "documents", "subdir", "9692bb55e1e2eec083333ee2139137e6ddf3a4d8.json").toFile(), Cord19Document.class);
        assertThat(document).isNotNull();
    }
}
