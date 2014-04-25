package com.greglturnquist

@Log
class Crawler implements CommandLineRunner {

    def levels = 0
    def level_limit = -1

    def strip(link) {
        if (link.endsWith("/")) {
            link[0..-2]
        } else {
            link
        }
    }

    def convert(link, domain) {      
        if (link.startsWith("http")) {
            return strip(link)
        }
    
        if (link.startsWith("//")) {
            return strip("http:${link}")
        }
    
        if (link.startsWith("/")) {
            def l = strip("http://${domain}${link}")
            //log.info "Returning ${l}"
            return l
        }
    
        return strip("http://${link}")
    }

    def scan_for_links(url, links, dead_links, paths, domain, 
        excluded_domains) {
        levels += 1
        if (level_limit > 0 && levels > level_limit) {
            log.info "You have exceeded the limit of ${level_limit}. Dropping back"
            return
        }

        log.info "Crawling ${url}"

        def html = url.toURL().text

        def local_links = []

        def m = ( html =~ /href="([a-zA-Z0-9\/\.\?-]+)"/ )
        //log.info "Found ${m.size()} links"

        for (link in m) {
            def converted_link = convert(link[1], domain)
            if (!links.contains(converted_link)) {
                //log.info "Adding ${converted_link} to list"
                links << converted_link
                local_links << converted_link
            } else {
                //log.info "${converted_link} is already in it!"
            }
        }

        for (link in local_links) {
            //log.info "Newly found! ${link}"
        }

        if (local_links.size() > 0) {
            for (link in local_links) {
                if (link.contains(domain)) {
                    def hostname = new URL(link).host
                    //log.info "Checking ${excluded_domains} for ${hostname}"
                    if (excluded_domains.contains(hostname)) {
                        log.info "${hostname} is on the excluded list, so skipping"
                        continue
                    } else {
                        //log.info "${hostname} is NOT excluded, so going deeper"
                    }
                    try {
                        scan_for_links(link, links, dead_links, paths,
                            domain, excluded_domains)
                        if (paths.containsKey(link)) {
                            paths[link] << url
                        } else {
                            paths[link] = [url]
                        }
                    } catch(/*urllib2.HTTPError*/ e) {
                        log.severe "Can't find link from ${url} to ${link}"
                        log.severe e.toString()
                        dead_links << [url, link]
                        //if (e.code == 404) {
                        //    dead_links << [url, link]
                        //    levels -= 1
                        //    return
                        //}
                    }
                } else {
                    log.info "${link} leaves this domain, so not crawling"
                }
            }
        } else {
            //log.info "No new links to crawl"
        }

        levels -= 1  
    }
    
    void run(String[] args) {    
        def links = []
        def dead_links = []
        def paths = [:]
        def domain = "spring.io"
        def excluded_domains = ["jira.spring.io", "forum.spring.io", "repo.spring.io"]
        scan_for_links("http://spring.io", links, dead_links, paths,
            domain, excluded_domains)
        log.info "============ GOOD ======================"
        links.sort()
        for (link in links) {
            log.info link
        }
        log.info "============ BAD ======================"
        for (link in dead_links) {
            log.info "${link[0]} -> ${link[1]}"
        }
        //log.info "============ PATHS ======================"
        //keys = paths.keys()
        //keys.sort()
        //for link in keys:
        //  log.info link
        //  for url in paths[link]:
        //    log.info "\t\t\t\t\t\t\t%s" % url
    }
}