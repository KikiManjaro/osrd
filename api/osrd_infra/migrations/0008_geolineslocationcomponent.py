# Generated by Django 3.2.5 on 2021-09-21 14:13

import django.contrib.gis.db.models.fields
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0007_alter_op'),
    ]

    operations = [
        migrations.CreateModel(
            name='GeoLinesLocationComponent',
            fields=[
                ('component_id', models.BigAutoField(primary_key=True, serialize=False)),
                ('geographic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
                ('schematic', django.contrib.gis.db.models.fields.MultiLineStringField(srid=3857)),
                ('entity', models.OneToOneField(on_delete=django.db.models.deletion.CASCADE, related_name='geo_lines_location', to='osrd_infra.entity')),
            ],
            options={
                'abstract': False,
            },
        ),
    ]