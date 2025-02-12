package cx.catapult.animals.web;


import cx.catapult.animals.domain.ApiError;
import cx.catapult.animals.domain.Horse;
import cx.catapult.animals.repository.AnimalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URL;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HorsesControllerIT {
    @LocalServerPort
    private int port;

    private URL base;

    private Horse horse;

    @Autowired
    private TestRestTemplate template;
    @Autowired
    private AnimalRepository animalRepository;

    @BeforeEach
    public void setUp() throws Exception {
        horse = new Horse("Spirit", "The Stallion");
        this.base = new URL("http://localhost:" + port + "/api/1/horses");
    }

    @Test
    void create_shouldInsertWhenValidRequest() {
        ResponseEntity<Horse> response = template.postForEntity(base.toString(), horse, Horse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotEmpty();
        assertThat(response.getBody().getName()).isEqualTo(horse.getName());
        assertThat(response.getBody().getDescription()).isEqualTo(horse.getDescription());
        assertThat(response.getBody().getGroup()).isEqualTo(horse.getGroup());

        animalRepository.deleteById(Long.parseLong(response.getBody().getId()));
    }

    @Test
    void create_shouldReturnUnsupportedMediaRequestWhenRequestIsNull() {
        ResponseEntity response = template.postForEntity(base.toString(), null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void create_shouldReturnBadRequestWhenNameInRequestIsNull() {
        horse.setName(null);

        ResponseEntity<ApiError> response = template.postForEntity(base.toString(), horse, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Name cannot be null or empty");
    }

    @Test
    void create_shouldReturnBadRequestWhenNameInRequestIsBlank() {
        horse.setName("");

        ResponseEntity<ApiError> response = template.postForEntity(base.toString(), horse, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Name cannot be null or empty");
    }

    @Test
    void create_shouldReturnBadRequestWhenDescriptionInRequestIsNull() {
        horse.setDescription(null);

        ResponseEntity<ApiError> response = template.postForEntity(base.toString(), horse, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Description cannot be null or empty");
    }

    @Test
    void create_shouldReturnBadRequestWhenDescriptionInRequestIsBlank() {
        horse.setDescription("");

        ResponseEntity<ApiError> response = template.postForEntity(base.toString(), horse, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Description cannot be null or empty");
    }

    @Test
    void get_shouldGetValidRecord() {
        ResponseEntity<Horse> response = template.postForEntity(base.toString(), horse, Horse.class);

        response = template.getForEntity(base.toString() + "/" + response.getBody().getId(), Horse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isNotEmpty();
        assertThat(response.getBody().getName()).isEqualTo(horse.getName());
        assertThat(response.getBody().getDescription()).isEqualTo(horse.getDescription());
        assertThat(response.getBody().getGroup()).isEqualTo(horse.getGroup());

        animalRepository.deleteById(Long.parseLong(response.getBody().getId()));
    }

    @Test
    void get_shouldReturnBadRequestWhenIdIsNull() {
        ResponseEntity<ApiError> response = template.getForEntity(base.toString() + "/ ", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Id cannot be null");
    }

    @Test
    void get_shouldReturnNotFoundWhenIdIsNotAvailable() {
        ResponseEntity<ApiError> response = template.getForEntity(base.toString() + "/1", ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Record not found");
    }

    @Test
    public void all_shouldReturnExistingRecords() {
        ResponseEntity<Horse> response = template.postForEntity(base.toString(), horse, Horse.class);

        Collection items = template.getForObject(base.toString(), Collection.class);
        assertThat(items.size()).isGreaterThanOrEqualTo(1);

        animalRepository.deleteById(Long.parseLong(response.getBody().getId()));
    }

    @Test
    void delete_shouldDeleteValidRecord() {
        ResponseEntity<Horse> response = template.postForEntity(base.toString(), horse, Horse.class);

        ResponseEntity<Void> deleteResponse = template.exchange(base.toString() + "/" + response.getBody().getId(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity getResponse = template.getForEntity(base.toString() + "/1", ApiError.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_shouldReturnBadRequestWhenIdIsNull() {
        ResponseEntity<ApiError> response = template.exchange(base.toString() + "/ ", HttpMethod.DELETE, null, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Id cannot be null");
    }

    @Test
    void delete_shouldReturnNotFoundWhenIdIsNotAvailable() {
        ResponseEntity<ApiError> response = template.exchange(base.toString() + "/1", HttpMethod.DELETE, null, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Record not found");
    }

    @Test
    void update_shouldUpdateIfRecordExists() {
        ResponseEntity<Horse> response = template.postForEntity(base.toString(), horse, Horse.class);
        horse = response.getBody();
        horse.setName("Warrior");

        ResponseEntity<Void> updateResponse = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), Void.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        response = template.getForEntity(base.toString() + "/" + response.getBody().getId(), Horse.class);

        assertThat(response.getBody().getName()).isEqualTo(horse.getName());
        animalRepository.deleteById(Long.parseLong(response.getBody().getId()));
    }

    @Test
    void update_shouldNotUpdateIfRecordDoesntExists() {
        ResponseEntity<ApiError> response = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Record not found");
    }

    @Test
    void update_shouldReturnUnsupportedMediaRequestWhenRequestIsNull() {
        ResponseEntity response = template.exchange(base.toString(), HttpMethod.PUT, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void update_shouldReturnBadRequestWhenNameInRequestIsNull() {
        horse.setName(null);

        ResponseEntity<ApiError> response = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Name cannot be null or empty");
    }

    @Test
    void update_shouldReturnBadRequestWhenNameInRequestIsBlank() {
        horse.setName("");

        ResponseEntity<ApiError> response = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Name cannot be null or empty");
    }

    @Test
    void update_shouldReturnBadRequestWhenDescriptionInRequestIsNull() {
        horse.setDescription(null);

        ResponseEntity<ApiError> response = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Description cannot be null or empty");
    }

    @Test
    void update_shouldReturnBadRequestWhenDescriptionInRequestIsBlank() {
        horse.setDescription("");

        ResponseEntity<ApiError> response = template.exchange(base.toString(), HttpMethod.PUT, new HttpEntity<>(horse), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Description cannot be null or empty");
    }
}
