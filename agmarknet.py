# market_service.py

from flask import Flask, request, jsonify
import requests
from datetime import datetime
from urllib.parse import unquote
from collections import Counter

app = Flask(__name__)

API_KEY = "579b464db66ec23bdd000001cdd3946e44ce4aad7209ff7b23ac571b"
BASE_URL = "https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070"

def format_specific_response(record):
    """Formats a detailed response for a single, specific record."""
    return (
        f"Based on the latest data from today, {record.get('arrival_date')}, "
        f"the going price for {record.get('commodity')} at the {record.get('market')} market "
        f"in {record.get('district')}, {record.get('state')} is around ₹{record.get('modal_price')} per quintal."
    )

def format_summary_response(records, state, district=None, market=None):
    """Formats a summary for broad queries."""
    if not records:
        return f"I couldn't find any market data for the specified location."

    market_counts = Counter(r['market'] for r in records)
    commodity_counts = Counter(r['commodity'] for r in records)
    
    top_markets = [m for m, c in market_counts.most_common(3)]
    top_commodities = [c for c, count in commodity_counts.most_common(3)]
    
    location = state
    if district: location = f"{district}, {state}"
    if market: location = f"the {market} market"

    price_strings = [f"{c} is around ₹{next((r.get('modal_price') for r in records if r['commodity'] == c), 'N/A')}" for c in top_commodities]

    return (
        f"Here is a summary for {location}. "
        f"The most active markets appear to be {', '.join(top_markets)}. "
        f"Key commodities being traded include {', '.join(top_commodities)}. "
        f"Current prices per quintal are: {', '.join(price_strings)}."
    )

@app.route("/market-price-latest", methods=["GET"])
def market_price_latest():
    params = {"api-key": API_KEY, "format": "json", "limit": 100}
    
    # Dynamically add filters from request arguments
    user_state = unquote(request.args.get("state", ""))
    user_district = unquote(request.args.get("district", ""))
    user_market = unquote(request.args.get("market", ""))
    user_commodity = unquote(request.args.get("commodity", ""))

    # State is the only truly required parameter for a meaningful search
    if not user_state:
        return jsonify({"error": "The 'state' parameter is required"}), 400
        
    params["filters[state.keyword]"] = user_state
    if user_district: params["filters[district]"] = user_district
    if user_market: params["filters[market]"] = user_market
    if user_commodity: params["filters[commodity]"] = user_commodity

    try:
        resp = requests.get(BASE_URL, params=params, timeout=15)
        resp.raise_for_status()
        data = resp.json()
        records = data.get("records", [])

        if not records:
            return jsonify({"error": "no_data_found"}), 404

        # A query is "specific" if it targets a particular commodity and market
        is_specific_query = user_market and user_commodity
        
        if is_specific_query:
            latest_record = max(records, key=lambda x: datetime.strptime(x.get("arrival_date", "01/01/1970"), "%d/%m/%Y"))
            final_response = {"market_info": format_specific_response(latest_record)}
        else:
            final_response = {"market_info": format_summary_response(records, user_state, user_district, user_market)}

        return jsonify(final_response)

    except Exception as e:
        return jsonify({"error": "api_call_failed", "details": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5004, debug=True)