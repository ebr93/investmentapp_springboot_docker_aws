import stockObject from "./stockobject";
import intList from "./investmentsArray";

const orderAPI = (() => {

  // same function signature; you can pass symbols optionally
  async function getQuote(symbols = ["AMD","IBM","AAPL","TSLA","AMZN","MSFT","GOOGL"]) {
    try {
      const symbolParam = symbols.join(",");

      // Call YOUR backend (not RapidAPI directly)
      const response = await fetch(`/api/market/quotes?region=US&symbols=${encodeURIComponent(symbolParam)}`, {
        method: "GET",
        headers: { "Accept": "application/json" }
      });

      if (!response.ok) {
        const body = await response.text();
        throw new Error(`getQuote(): backend failed (${response.status}) ${body}`);
      }

      const responseData = await response.json();
      console.log(responseData);

      // RapidAPI response shape preserved: quoteResponse.result[]
      const stockData = responseData.quoteResponse;

      // Clear old list if you want fresh data each time (optional)
      // intList.length = 0;

      for (let i = 0; i < Math.min(7, stockData.result.length); i++) {
        const r = stockData.result[i];

        // IMPORTANT: "ask" is often null. Prefer regularMarketPrice fallback.
        const price = (r.ask != null) ? r.ask : r.regularMarketPrice;

        const newStockObject = new stockObject(
          r.shortName,
          r.symbol,
          price,
          r.regularMarketChangePercent
        );

        intList.push(newStockObject);
        console.log(`Inside Loop + getQuote(): ${intList}`);
      }

      console.log(`Outside Loop + getQuote(): ${intList}`);

      return responseData;
    } catch (err) {
      console.error("getQuote():", err);
      throw err; // let caller handle errors too
    }
  }

  return { getQuote };
})();

export default orderAPI;
