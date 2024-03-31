package org.webcrawler.Operations

import groovyx.net.http.HttpBuilder
import groovyx.net.http.optional.Download
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class Operation {

    private static Document buscarPagina(String url) throws IOException {
        return (Document) HttpBuilder.configure { request.uri = url }.get()
    }

    static String PaginaTISS() {

        Document page = buscarPagina("https://www.gov.br/ans/pt-br")
        Element content = page.getElementById("ce89116a-3e62-47ac-9325-ebec8ea95473")
        String url = content.getElementsByTag("a").attr("href")

        Document page2 = buscarPagina(url)
        Element content2 = page2.getElementsByClass("govbr-card-content").first()
        return content2.getElementsByTag("a").attr("href")
    }

    static void buscarTabela() {

        Document page = buscarPagina(PaginaTISS())
        Element content = page.getElementsByClass("internal-link").first()
        String url = content.attr("href")

        Document page2 = buscarPagina(url)
        Element table = page2.getElementsByTag("tbody").first().getElementsByTag("tr").last()
        url = table.lastElementChild().firstElementChild().attr("href")

        baixarArquivo(url, "Documento_do_TISS.zip")

    }

    void buscarHistorico() {
        try {
            Document page = Jsoup.connect(PaginaTISS()).get()
            Element content = page.select(".external-link").get(0)
            String url = content.select("a").attr("href")

            Document page2 = Jsoup.connect(url).get()
            Elements content2 = page2.select("tbody")
            Elements listaTr = content2.select("tr")

            List<List<String>> informacoes = []

            informacoes.add(["Competência", "Publicação", "Início de Vigência"])

            listaTr.each { tr ->
                Elements listaTd = tr.select("td")
                String competencia = listaTd.get(0).text()

                List<String> competenciaSplit = competencia.split("/")
                Integer ano = Integer.parseInt(competenciaSplit[1])

                if (ano >= 2016) {
                    String publicacao = listaTd.get(1).text()
                    String inicioVigencia = listaTd.get(2).text()
                    informacoes.add([competencia, publicacao, inicioVigencia])
                }
            }

            criarArquivo(informacoes, "./Downloads/historico_versoes_TISS.txt")
        } catch (Exception e) {
            println("Erro ao coletar informações: ${e.getMessage()}")
        }
    }


    static void buscarTabelaErros() {

        try {
            Document page = Jsoup.connect(PaginaTISS()).get()
            Element content = page.select("#parent-fieldname-text > .callout").get(2)
            String url = content.select("a").attr("href")

            Document page2 = buscarPagina(url)
            Element content2 = page2.select("#parent-fieldname-text").get(0)
            url = content2.select("a").attr("href")

            baixarArquivo(url, "tabela_de_erros_ANS.xlsx")

        }catch (Exception e) {
            println("Erro ao captar os dados: " + e.getMessage())
        }
    }

     void criarArquivo(List<List<String>> data, String path ) {

         try {
             File file = new File(caminhoArquivo)
             if (file.exists()) {
                 file.delete()
             }
             file.createNewFile()
             file.withWriter { writer ->
                 data.each { info ->
                     writer.writeLine(info.join(", "))
                 }
             }
             println("Arquivo salvo em ${path}")
         } catch (Exception e) {
             println("Erro ao criar arquivo: ${e.getMessage()}")
         }
    }

    private static void baixarArquivo(String url, String name) {

        File directory = new File("./Downloads")
        directory.mkdirs()
        File path = new File(directory, name)

        HttpBuilder.configure {
            request.uri = url
        }.get { Download.toFile(delegate, path)}
    }
}


