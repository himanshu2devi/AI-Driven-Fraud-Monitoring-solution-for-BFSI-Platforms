import requests

url = "http://localhost:8000/predict"

data = {
    "features": [12000, 1, 1, 7, 1, 200, 3]
}

response = requests.post(url, json=data)

print("Response:", response.json())