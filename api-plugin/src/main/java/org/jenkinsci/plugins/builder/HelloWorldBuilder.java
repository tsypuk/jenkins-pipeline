package org.jenkinsci.plugins.builder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.jenkinsci.plugins.domain.Book;
import org.jenkinsci.plugins.domain.BookJsonConvertor;
import org.jenkinsci.plugins.domain.BooksResource;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

public class HelloWorldBuilder extends Builder implements SimpleBuildStep {

    private final String url;

    private final BookJsonConvertor bookJsonConvertor = new BookJsonConvertor();

    @DataBoundConstructor
    public HelloWorldBuilder(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);

        request.addHeader("User-Agent", "Jenkins");
        HttpResponse response = client.execute(request);

        listener.getLogger().printf("Response Code : {}", response.getStatusLine().getStatusCode());

        BooksResource booksResource = bookJsonConvertor.readBooks(readResult(response));
        final List<Book> books = booksResource.getBooks();
        final boolean error = booksResource.isError();

        listener.getLogger().print("API response");
        books.forEach(book -> listener.getLogger().printf(book.toString()));
        listener.getLogger().printf("Status error: {}", error);
    }

    private String readResult(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    @Symbol("api")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }

}
