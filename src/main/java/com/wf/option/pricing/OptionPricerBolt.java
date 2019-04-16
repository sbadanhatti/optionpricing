package com.wf.option.pricing;

import java.util.List;

import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.jquantlib.Settings;
import org.jquantlib.daycounters.Actual365Fixed;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.EuropeanExercise;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.EuropeanOption;
import org.jquantlib.instruments.Option;
import org.jquantlib.instruments.Payoff;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.instruments.VanillaOption;
import org.jquantlib.processes.BlackScholesMertonProcess;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.termstructures.BlackVolTermStructure;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.termstructures.volatilities.BlackConstantVol;
import org.jquantlib.termstructures.yieldcurves.FlatForward;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.Date;
import org.jquantlib.time.calendars.Target;

import com.google.gson.JsonObject;

public class OptionPricerBolt extends BaseBasicBolt {

	public void execute(Tuple tuple, BasicOutputCollector collector) {
		List<Object> values = tuple.getValues();
		JsonObject data = (JsonObject) values.get(0);
		double optionPrice = price(data);
	}

	private double price(JsonObject data) {
		final Option.Type type = Option.Type.Call;

	       final double underlying = 197.0;
	       /*@Rate*/final double riskFreeRate = 0.0256;
	       final double volatility = data.get("impliedVol").getAsDouble()();
	       final double dividendYield = 0.00;
	       // set up dates
	       final Calendar calendar = new Target();
	       final Date todaysDate = new Date(new java.util.Date());
	       new Settings().setEvaluationDate(todaysDate);
	       
	       Long lDate = data.get("expiryDate").getAsLong();
	       final Date expiryDate = new Date(new java.util.Date(lDate));
	       final DayCounter dayCounter = new Actual365Fixed();
	       final Exercise europeanExercise = new EuropeanExercise(expiryDate);

	       // bootstrap the yield/dividend/volatility curves
	       final Handle<Quote> underlyingH = new Handle<Quote>(new SimpleQuote(underlying));
	       final Handle<YieldTermStructure> flatDividendTS = new Handle<YieldTermStructure>(new FlatForward(todaysDate, dividendYield, dayCounter));
	       final Handle<YieldTermStructure> flatTermStructure = new Handle<YieldTermStructure>(new FlatForward(todaysDate, riskFreeRate, dayCounter));
	       final Handle<BlackVolTermStructure> flatVolTS = new Handle<BlackVolTermStructure>(new BlackConstantVol(todaysDate, calendar, volatility, dayCounter));
	       final Payoff payoff = new PlainVanillaPayoff(type, data.get("optionStrike").getAsDouble());

	       final BlackScholesMertonProcess bsmProcess = new BlackScholesMertonProcess(underlyingH, flatDividendTS, flatTermStructure, flatVolTS);

	       // European Options
	       final VanillaOption europeanOption = new EuropeanOption(payoff, europeanExercise);
	       // Black-Scholes for European
	       return europeanOption.NPV();
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("optionPrice"))		;
	}

}