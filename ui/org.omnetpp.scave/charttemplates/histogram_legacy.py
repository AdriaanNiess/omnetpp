from omnetpp.scave import results, chart

params = chart.get_configured_properties()

# This expression selects the results

filter_expression = params["filter"]

# The data is returned as a Pandas DataFrame
df = results.get_histograms(filter_expression, include_attrs=True, include_itervars=True)

# You can perform any transformations on the data here

print(df)
# Finally, the results are plotted
chart.plot_histograms(df)

chart.copy_properties()
