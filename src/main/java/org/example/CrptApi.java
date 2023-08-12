package org.example;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.concurrent.TimeUnit;

public class CrptApi {

    //для получения токена
    private String uuid;
    //для ввода в оборот товара
    private String token;
    //тип (рф)
    private final String type = "LP_INTRODUCE_GOODS";
    //товарная группа
    private final String pg = "product group string";
    //данная коллекция нужна для сохранения времени созданных потоков
    final Queue<Long> requests;
    private String value;
    private final TimeUnit timeUnit;
    private final int requestLimit;

    CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requests = new LinkedList<>();
    }

    // метод ввода товара в оборот
    public synchronized void createDocument(Document document, String signature) throws IOException, InterruptedException {
        apiRequest();
        GsonBuilder gsonBuilder = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
               @Override
               public boolean shouldSkipField(FieldAttributes field) {
                   if (field == null)
                       return true;
                   return false;
               }

               @Override
               public boolean shouldSkipClass(Class<?> aClass) {
                   return false;
               }
           }
        );
        Gson gson = gsonBuilder.create();
        gson.toJson(document);
        String json = gson.toJson(document);
        json = toBase64(json);
        signature = toBase64(signature);
        String putGoods = "curl 'https://ismp.crpt.ru/api/v3/lk/documents/create?pg= " + pg + "' \\ -H 'content-type: application/json' -H 'Authorization: Bearer " + token + "' --data-binary '{ \"product_document\":\"" + json + "\",\\ \"document_format\":\"MANUAL\", type: \"" + type + "\",\\ \"signature\":\"" + signature + "\"}'";
        value = processExecutor(putGoods);
    }

    // метод ограничения запросов
    private synchronized void apiRequest() throws InterruptedException {
        final long current = System.currentTimeMillis();
        requests.add(current);
        if (current - requests.peek() >= timeUnit.toMillis(1)) {
            requests.clear();
            requests.add(current);
        }
        if (requests.size() >= requestLimit) {
            long first = requests.peek();

            final int difference = (int) (current - first);

            if (difference <= timeUnit.toMillis(1)) {
                final long toSleep = timeUnit.toMillis(1) - difference;
                timeUnit.MILLISECONDS.sleep(toSleep);
            }
        }
    }

    // метод запроса авторизации(оставлен публичным, потому что за время действия токена, его можно использовать несколько раз)
    public synchronized void getAuth() throws IOException {
        String getAuth = "curl -v https://ismp.crpt.ru/api/v3/auth/cert/key";
        uuid = processExecutor(getAuth);
        uuid = uuid.substring(uuid.indexOf("uuid"));
        uuid = uuid.substring(uuid.indexOf("\"") + 3, uuid.indexOf(",") - 1);
    }

    // метод получения аутентификационного токена (оставлен публичным, потому что за время действия токена, его можно использовать несколько раз)
    public synchronized void getToken(String s) throws IOException {
        s = toBase64(s);
        String getTokenCommand = "curl -X POST -v 'https://ismp.crpt.ru/api/v3/auth/cert/'\\" +
                "-H 'content-type: application/json;charset=UTF-8'\\" +
                "--data-binary '{\\" +
                "\"uuid\":\"" + uuid + "\",\\" +
                "\"data\":\"" + s + "\"'";
        token = processExecutor(getTokenCommand);
        token = token.substring(token.indexOf("token"));
        token = token.substring(token.indexOf("\"") + 3, token.indexOf("}") - 1);
    }

    // метод кодировки в base64
    private synchronized String toBase64(String text) {
        Base64.Encoder enc = Base64.getEncoder();
        byte[] encbytes = enc.encode(text.getBytes());
        StringBuilder textBuilder = new StringBuilder();

        for (byte encbyte : encbytes) {
            textBuilder.append((char) encbyte);
        }
        text = textBuilder.toString();
        return text;
    }

    private synchronized String processExecutor(String command) throws IOException {
        String text;
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuffer data = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            data.append(line);
        }
        text = data.toString();
        return text;
    }

    @JsonAutoDetect
    static class Document {
        @Expose
        private List<Description> descriptions;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        @Expose
        private String importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        @Expose
        private String reg_number;

        Document(List<Description> descriptions, String doc_id, String doc_status, String doc_type, String importRequest, String owner_inn,
                 String participant_inn, String producer_inn, String production_date, String production_type,
                 List<Product> products, String reg_date, String reg_number) {
            this.descriptions = descriptions;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        @JsonAutoDetect
        static class Description {
            Description(String participant_inn) {
                this.participant_inn = participant_inn;
            }

            private String participant_inn;
        }

        @JsonAutoDetect
        static class Product {
            public Product(String certificate_document, String certificate_document_date, String certificate_document_number,
                           String owner_inn, String production_date, String tnved_code, String uit_code, String uitu_code) {
                this.certificate_document = certificate_document;
                this.certificate_document_date = certificate_document_date;
                this.certificate_document_number = certificate_document_number;
                this.owner_inn = owner_inn;
                this.production_date = production_date;
                this.tnved_code = tnved_code;
                this.uit_code = uit_code;
                this.uitu_code = uitu_code;
            }

            @Expose
            private String certificate_document;
            @Expose
            private String certificate_document_date;
            @Expose
            private String certificate_document_number;
            private String owner_inn;
            private String production_date;
            private String tnved_code;
            @Expose
            private String uit_code;
            @Expose
            private String uitu_code;
        }
    }
}