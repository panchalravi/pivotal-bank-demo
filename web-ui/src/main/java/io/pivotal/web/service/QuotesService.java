package io.pivotal.web.service;

import java.util.*;

import io.pivotal.web.domain.CompanyInfo;
import io.pivotal.web.domain.Order;
import io.pivotal.web.domain.Portfolio;
import io.pivotal.web.domain.Quote;
import io.pivotal.web.exception.OrderNotSavedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@Service
@RefreshScope
public class QuotesService {
	private static final Logger logger = LoggerFactory
			.getLogger(QuotesService.class);
	@Autowired
	@LoadBalanced
	private RestTemplate restTemplate;

	@Value("${pivotal.downstream-protocol:http}")
	protected String downstreamProtocol;

	@Value("${pivotal.quotesService.name}")
	private String quotesService;
	
	@HystrixCommand(fallbackMethod = "getQuoteFallback")
	public Quote getQuote(String symbol) {
		logger.debug("Fetching quote: " + symbol);
		List<Quote> quotes = getMultipleQuotes(symbol);
		if (quotes.size() == 1 ) {
			logger.debug("Fetched quote: " + quotes.get(0));
			return quotes.get(0);
		}
		logger.debug("exception: should only be 1 quote and got multiple or zero: " + quotes.size());
		return new Quote();
	}
	
	private Quote getQuoteFallback(String symbol) {
		logger.debug("Fetching fallback quote for: " + symbol);
		Quote quote = new Quote();
		quote.setSymbol(symbol);
		quote.setStatus("FAILED");
		return quote;
	}
	@HystrixCommand(fallbackMethod = "getCompaniesFallback")
	public List<CompanyInfo> getCompanies(String name) {
		logger.debug("Fetching companies with name or symbol matching: " + name);
		CompanyInfo[] infos = restTemplate.getForObject(downstreamProtocol + "://" + quotesService + "/v1/company/{name}", CompanyInfo[].class, name);
		return Arrays.asList(infos);
	}
	/*
	* @return
	*/
	public String killInstance() {
		return restTemplate.getForObject(downstreamProtocol + "://" + quotesService + "/v1/basics?doit=true", String.class);
	}

	@HystrixCommand(fallbackMethod = "getInstanceInfoFallback")
	public Map<String, Object> getInstanceInfo() {
		logger.debug("Fetching quotes service instance info... ");
		Map<String, Object> instanceInfo = restTemplate.getForObject(downstreamProtocol + "://" + quotesService + "/v1/basics", Map.class);
		return instanceInfo;
	}

	public Map<String, Object> getInstanceInfoFallback() {
		logger.debug("Fetching default quotes service instance info...");
		Map<String, Object> instanceInfo = new HashMap<>();
		instanceInfo.put("applicationName", "");
		instanceInfo.put("instanceIndex", "0");
		instanceInfo.put("instanceAddr", "0.0.0.0");
		instanceInfo.put("containerAddr", "0.0.0.0");
		return instanceInfo;
	}

	/**
	 * @param name
	 * @return
	 */
	private List<CompanyInfo> getCompaniesFallback(String name) {
		List<CompanyInfo> infos = new ArrayList<>();
		return infos;
	}
	/**
	 * Retrieve multiple quotes.
	 * 
	 * @param symbols comma separated list of symbols.
	 * @return
	 */
	public List<Quote> getMultipleQuotes(String symbols) {
		logger.debug("retrieving multiple quotes: " + symbols);
		Quote[] quotesArr = restTemplate.getForObject(downstreamProtocol + "://" + quotesService + "/v1/quotes?q={symbols}", Quote[].class, symbols);
		List<Quote> quotes = Arrays.asList(quotesArr);
		logger.debug("Received quotes: {}",quotes);
		return quotes;
		
	}
	/**
	 * Retrieve multiple quotes.
	 * 
	 * @param symbols
	 * @return
	 */
	public List<Quote> getMultipleQuotes(String[] symbols) {
		logger.debug("Fetching multiple quotes array: {} ",Arrays.asList(symbols));
		
		return getMultipleQuotes(Arrays.asList(symbols));
	}
	/**
	 * Retrieve multiple quotes.
	 * 
	 * @param symbols
	 * @return
	 */
	@HystrixCommand(fallbackMethod = "getMultipleQuotesFallback")
	public List<Quote> getMultipleQuotes(Collection<String> symbols) {
		logger.debug("Fetching multiple quotes array: {} ",symbols);
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> i = symbols.iterator(); i.hasNext();) {
			builder.append(i.next());
			if (i.hasNext()) {
				builder.append(",");
			}
		}
		return getMultipleQuotes(builder.toString());
	}

	public List<Quote> getMultipleQuotesFallback(Collection<String> symbols) {
		logger.debug("Fetching multiple quotes array: {} ",symbols);
		List<Quote> quotes = new ArrayList<>();
		return quotes;
	}

}
