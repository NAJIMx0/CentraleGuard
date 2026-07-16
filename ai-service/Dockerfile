FROM python:3.13-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY consumer.py .
COPY anomaly_model.pkl .
ENTRYPOINT ["python", "consumer.py"]